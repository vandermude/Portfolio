#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <ctype.h>

#define NUM_ITERATIONS			2000
#define MAX_LINE				(4096 * 4)
#define TRUE					(0 == 0)
#define FALSE					(0 != 0)

typedef int	BOOLEAN;
typedef int	INTEGER	;
typedef double	REAL;
typedef char	CHAR;
typedef char*	STRING;
typedef struct {
	STRING		train_filnam;
	FILE*		train_fp;
	STRING		output_filnam;
	FILE*		output_fp;
}	Files;
/* All tuples are 3-tuples right now */
typedef struct {
	INTEGER		tuple_length;	/* Number of characters includeing terminating '\0' for each word */
	INTEGER		tuple_words;	/* Number of words */
	STRING		tuple_strings;	/* The words in the strings separated by '\0' */
}	Tuple;
typedef struct {
	INTEGER		num_chars;		/* Number of characters in training file */
	INTEGER		num_words;		/* Number of characters in training file */
	INTEGER		num_lines;		/* Number of lines in training file */
	INTEGER		num_pages;		/* Number of pages in training file */
	INTEGER		num_labels;		/* Number of labels in training file */
	INTEGER		num_tuples;		/* Number of tuples generated from text in training file */
	INTEGER		label_bufsiz;	/* Size of character buffer to store label names */
}	Stats;
typedef struct {
	INTEGER		tuple;
	REAL		alpha;
	REAL*		c1;
	REAL*		c0;
}	WeakLearner;

void read_data(Files* files, Stats* stats, INTEGER*** ptr_labels, INTEGER*** ptr_tuples,
	BOOLEAN** ptr_tuples_selected, Tuple** ptr_tuple_array, INTEGER** ptr_tuple_index,
	STRING** ptr_label_text);
void read_stats(Files* files, Stats* stats);
void init_data(Stats* stats, INTEGER*** ptr_labels, INTEGER*** ptr_tuples, BOOLEAN** ptr_tuples_selected,
	Tuple** ptr_tuple_array, INTEGER** ptr_tuple_index, STRING** ptr_label_text);
INTEGER count_words(STRING string);
INTEGER process_line(STRING string);
void process_labels(STRING string, Stats* stats, INTEGER*** ptr_labels, STRING** ptr_label_text);
void print_labels(Stats* stats, INTEGER** labels, STRING* label_text);
void process_tuples(INTEGER num_chars, INTEGER num_words, STRING string, Stats* stats,
	INTEGER*** ptr_tuples, BOOLEAN** ptr_tuples_selected, Tuple** ptr_tuple_array,
	INTEGER** ptr_tuple_index);
INTEGER tuple_find(Tuple* ptr_tuple, BOOLEAN** ptr_tuples_selected, Tuple** ptr_tuple_array,
	INTEGER** tuple_index, Stats* stats);
void output_labels(Files* files, Stats* stats, STRING* label_text);
void output_learner(INTEGER index, Files* files, Stats* stats, WeakLearner* weak_learner,
	Tuple* tuple_array);
void boostexter(Files* files, Stats* stats, INTEGER** labels, INTEGER** tuples,
	BOOLEAN* tuples_selected, STRING* label_text, Tuple* tuple_array);
void get_weak_learner(Stats* stats, WeakLearner* weak_learner, REAL* normalization, REAL* alpha,
	REAL** dist, INTEGER** labels, INTEGER** tuples, BOOLEAN* tuples_selected,
	REAL* c0, REAL* c1, REAL* min_c0, REAL* min_c1);

int main (int argc, STRING argv[])
{
	Files files = { NULL, NULL, NULL, NULL };
	Stats stats = { 0, 0, 0, 0, 0, 0, 0 };
	/* labels for each page: labels[l][p] = 1 if label l is a label for page p else -1 */
	INTEGER** labels = NULL;
	/* the label_text for each label */
	STRING* label_text = NULL;
	/* the unique 3-tuples in all the pages: tuples[t][p] = 1 if 3-tuple t is in page p */
	INTEGER** tuples = NULL;
	/* the array of tuples */
	Tuple* tuple_array = NULL;
	/* the array of tuples are entered as found tuple_index is the sorted index */
	INTEGER* tuple_index;
	/* set tuples_selected[t] = True if tupe t has been selected as a weak learner or should be skipped */
	BOOLEAN* tuples_selected = NULL;
	if (argc != 3)
	{
		printf("ERROR: Need training data file and output data file\n");
		exit(1);
	}
	files.train_filnam = argv[1];
	files.train_fp = fopen(files.train_filnam, "r");
	if (files.train_fp == NULL)
	{
		printf("ERROR: can't open file %s for input\n", files.train_filnam);
		exit(2);
	}
	files.output_filnam = argv[2];
	files.output_fp = fopen(files.output_filnam, "w");
	if (files.output_fp == NULL)
	{
		printf("ERROR: can't open file %s for output\n", files.output_filnam);
		exit(3);
	}
	read_data(&files, &stats, &labels, &tuples, &tuples_selected, &tuple_array, &tuple_index,
		&label_text);
	printf("stats.num_pages = %d\n", stats.num_pages);
	printf("stats.num_labels = %d\n", stats.num_labels);
	printf("stats.num_tuples = %d\n", stats.num_tuples);
	//print_labels(&stats, labels, label_text);
	boostexter(&files, &stats, labels, tuples, tuples_selected, label_text, tuple_array);
}

void read_data(Files* files, Stats* stats, INTEGER*** ptr_labels, INTEGER*** ptr_tuples,
	BOOLEAN** ptr_tuples_selected, Tuple** ptr_tuple_array, INTEGER** ptr_tuple_index,
	STRING** ptr_label_text)
{
	CHAR linebufr[MAX_LINE];
	INTEGER num_chars;
	INTEGER num_words;
	read_stats(files, stats);
	fclose(files->train_fp);
	files->train_fp = fopen(files->train_filnam, "r");
	/* Allocate data tables */
	init_data(stats, ptr_labels, ptr_tuples, ptr_tuples_selected, ptr_tuple_array, ptr_tuple_index,
		ptr_label_text);
	/* Read data file with lines: PAGE	URL	LABEL_1	LABEL_2	...	LABEL_n and associated */
	/* page for page */
	/* the list of labels for each page: page_labels[p] = [ "LABEL1", "LABEL2", ... ] */
	/* Reset the number of labels */
	stats->num_labels = 0;
	stats->num_tuples = 0;
	stats->num_pages = 0;
	while (fgets(linebufr, MAX_LINE, files->train_fp) != NULL)
	{
		if (strncmp(linebufr, "PAGE", 4) == 0)
		{
			/* Read and parse PAGE line into page_labels */
			/* Note: label_field[1] = URL for PAGE */
if((stats->num_pages % 10) == 0) printf("page = %d\n", stats->num_pages);
			process_labels(linebufr, stats, ptr_labels, ptr_label_text);
			stats->num_pages++;
		} else {
			num_chars = strlen(linebufr);
			num_words = process_line(linebufr);
			process_tuples(num_chars, num_words, linebufr, stats,
				ptr_tuples, ptr_tuples_selected, ptr_tuple_array, ptr_tuple_index);
		}
	}
printf("page = %d\n", stats->num_pages);
	fclose(files->train_fp);
}

void read_stats(Files* files, Stats* stats)
{
	CHAR linebufr[MAX_LINE];
	BOOLEAN longline;
	/* Read training file. Count lines, characters, pages */
	/* Label text buffer is no more than the size of the lines beginning with "PAGE" */
	/* Tuple text buffer is no more than the size of the input text */
	stats->num_chars = 0;
	stats->num_words = 0;
	stats->num_lines = 0;
	stats->num_pages = 0;
	stats->num_labels = 0;
	stats->num_tuples = 0;
	stats->label_bufsiz = 0;
	longline = FALSE;
	while (fgets(linebufr, MAX_LINE, files->train_fp) != NULL)
	{
		if (strlen(linebufr) >= (MAX_LINE - 2))
		{
			printf("ERROR: input line %d greater than %d - increase MAX_LINE\n",
				(stats->num_lines + 1), MAX_LINE);
			longline = TRUE;
		}
		stats->num_chars += strlen(linebufr) + 1;
		stats->num_words += count_words(linebufr);
		stats->num_lines++;
		if (strncmp(linebufr, "PAGE", 4) == 0)
		{
			stats->num_pages++;
			stats->label_bufsiz += strlen(linebufr) + 1;
			stats->num_labels += count_words(linebufr);
		} else {
			stats->num_tuples += count_words(linebufr);
		}
	}
	if (longline == TRUE)
	{
		printf("ERROR: long lines - aborting\n");
		exit(1);
	}
}

void init_data(Stats* stats, INTEGER*** ptr_labels, INTEGER*** ptr_tuples, BOOLEAN** ptr_tuples_selected,
	Tuple** ptr_tuple_array, INTEGER** ptr_tuple_index, STRING** ptr_label_text)
{
	INTEGER l;
	INTEGER p;
	INTEGER t;
	/* labels for each page: labels[l][p] = 1 if label l is a label for page p else -1 */
	*ptr_labels = malloc(stats->num_labels * sizeof(INTEGER*));
	for (l = 0; l < stats->num_labels; l++)
	{
		(*ptr_labels)[l] = malloc(stats->num_pages * sizeof(INTEGER));
		for (p = 0; p < stats->num_pages; p++)
		{
			(*ptr_labels)[l][p] = -1;
		}
	}
	/* the label_text for each label */
	*ptr_label_text = malloc(stats->num_labels * sizeof(STRING));
	for (l = 0; l < stats->num_labels; l++)
	{
		(*ptr_label_text)[l] = NULL;
	}
	(*ptr_label_text)[0] = malloc(stats->label_bufsiz * sizeof(CHAR));
	/* the array of tuples */
	*ptr_tuple_array = malloc(stats->num_tuples * sizeof(Tuple));
	for (t = 0; t < stats->num_tuples; t++)
	{
		(*ptr_tuple_array)[t].tuple_length = 0;
		(*ptr_tuple_array)[t].tuple_words = 0;
		(*ptr_tuple_array)[t].tuple_strings = NULL;
	}
	(*ptr_tuple_array)[0].tuple_length = 0;
	(*ptr_tuple_array)[0].tuple_words = 0;
	(*ptr_tuple_array)[0].tuple_strings = malloc(stats->num_chars * sizeof(CHAR));
	/* the unique 3-tuples in all the pages: tuples[t][p] = 1 if 3-tuple t is in page p */
	*ptr_tuples = malloc(stats->num_tuples * sizeof(INTEGER*));
	for (t = 0; t < stats->num_tuples; t++)
	{
		(*ptr_tuples)[t] = malloc(stats->num_pages * sizeof(INTEGER));
		for (p = 0; p < stats->num_pages; p++)
		{
			(*ptr_tuples)[t][p] = 0;
		}
	}
	/* set tuples_selected[t] = True if tupe t has been selected as a weak learner */
	*ptr_tuples_selected = malloc(stats->num_tuples * sizeof(BOOLEAN));
	for (t = 0; t < stats->num_tuples; t++)
	{
		(*ptr_tuples_selected)[t] = FALSE;
	}
	/* the array of tuples are entered as found tuple_index is the sorted index */
	*ptr_tuple_index = malloc(stats->num_tuples * sizeof(INTEGER));
	for (t = 0; t < stats->num_tuples; t++)
	{
		(*ptr_tuple_index)[t] = 0;
	}
}

INTEGER count_words(STRING string)
{
	INTEGER ret_valu;
	INTEGER i;
	ret_valu = 0;
	if (isspace(*string) != 0)
	{
		ret_valu++;
	}
	for ( ; *string != '\0'; string++)
	{
		if ((isspace(string[0])) && (!isspace(string[1])))
		{
			ret_valu++;
		}
	}
	return(ret_valu);
}

/* Replace all runs of white-space by nulls. Return number of words */
INTEGER process_line(STRING string)
{
	INTEGER ret_valu;
	BOOLEAN was_space;
	STRING send;
	STRING recv;
	send = string;
	recv = string;
	while ((isspace(*send)) && (send != '\0'))
	{
		send++;
	}
	was_space = FALSE;
	ret_valu = 0;
	while (*send != '\0')
	{
		if (isspace(*send))
		{
			if (was_space == FALSE)
			{
				*recv++ = '\0';
				ret_valu++;
			}
			send++;
			was_space = TRUE;
		} else {
			*recv++ = *send++;
			was_space = FALSE;
		}
	}
	return(ret_valu);
}

void process_labels(STRING string, Stats* stats, INTEGER*** ptr_labels, STRING** ptr_label_text)
{
	INTEGER i;
	INTEGER j;
	INTEGER l;
	INTEGER tab_count;
	STRING label;
	STRING new_label;
	tab_count = 0;
	label = NULL;
	new_label = string;
	for (i = 0; string[i] != '\0'; i++)
	{
		if ((string[i] == '\t') || (string[i] == '\n'))
		{
			tab_count++;
			label = new_label;
			new_label = &(string[i + 1]);
			string[i] = '\0';
			if (tab_count >= 3)
			{
				for (l = 0; l < stats->num_labels; l++)
				{
					if (strcmp(label, (*ptr_label_text)[l]) == 0)
					{
						(*ptr_labels)[l][stats->num_pages] = 1;
						break;
					}
				}
				if (l == stats->num_labels)
				{
					/* add a new label */
					if (stats->num_labels > 0)
					{
						(*ptr_label_text)[stats->num_labels] =
							(*ptr_label_text)[stats->num_labels - 1] +
								strlen((*ptr_label_text)[stats->num_labels - 1]) + 1;
					}
					for (j = 0; label[j] != '\0'; j++)
					{
						(*ptr_label_text)[stats->num_labels][j] = label[j];
					}
					(*ptr_label_text)[stats->num_labels][j] = '\0';
					(*ptr_labels)[stats->num_labels][stats->num_pages] = 1;
					stats->num_labels++;
				}
			}
		}
	}
}

void process_tuples(INTEGER num_chars, INTEGER num_words, STRING string, Stats* stats,
	INTEGER*** ptr_tuples, BOOLEAN** ptr_tuples_selected, Tuple** ptr_tuple_array,
	INTEGER** ptr_tuple_index)
{
	INTEGER i;
	INTEGER found_index;
	Tuple tuple;
	CHAR tuple_string[MAX_LINE];
	STRING next_word;
	STRING next_bufr;
	memcpy((*ptr_tuple_array)[stats->num_tuples].tuple_strings, string, num_chars);
	string = (*ptr_tuple_array)[stats->num_tuples].tuple_strings;
	next_bufr = &(string[num_chars]);
	for (i = 0; i < num_words - 2; i++)
	{
		tuple.tuple_words = 3;
		tuple.tuple_strings = string;
		tuple.tuple_length = strlen(string) + 1;
		next_word = &(string[strlen(string) + 1]);
		tuple.tuple_length += strlen(next_word) + 1;
		next_word = &(next_word[strlen(next_word) + 1]);
		tuple.tuple_length += strlen(next_word) + 1;
		found_index = tuple_find(&tuple, ptr_tuples_selected, ptr_tuple_array, ptr_tuple_index, stats);
		(*ptr_tuples)[found_index][stats->num_pages - 1] = 1;
		string += strlen(string) + 1;
	}
	(*ptr_tuple_array)[stats->num_tuples].tuple_strings = next_bufr;
}

INTEGER tuple_find(Tuple* ptr_tuple, BOOLEAN** ptr_tuples_selected, Tuple** ptr_tuple_array,
	INTEGER** ptr_tuple_index, Stats* stats)
{
	INTEGER t;
	INTEGER imin;
	INTEGER imax;
	INTEGER imid;
	INTEGER tuple_size;
	INTEGER compare;
	imin = 0;
	imax = stats->num_tuples - 1;
	/* continue searching while [imin, imax] is not empty */
	while (imax >= imin)
	{
		/* calculate the midpoint for roughly equal partition */
		imid = (imin + imax) / 2;
		tuple_size = ptr_tuple->tuple_length;
		if (tuple_size > (*ptr_tuple_array)[(*ptr_tuple_index)[imid]].tuple_length)
		{
			tuple_size = (*ptr_tuple_array)[(*ptr_tuple_index)[imid]].tuple_length;
		}
		compare = memcmp(ptr_tuple->tuple_strings,
			(*ptr_tuple_array)[(*ptr_tuple_index)[imid]].tuple_strings, tuple_size);
		if(compare == 0)
		{
			/* key found at index imid */
			break;
		}
		/* determine which subarray to search */
		else if (compare < 0)
		{
			/* change max index to search lower subarray */
			imax = imid - 1;
		} else {
			/* change min index to search upper subarray */
			imin = imid + 1;
		}
	}
	if (imax >= imin)
	{
		/* imid is result */
		/* Set selected to FALSE so it can be considered */
		(*ptr_tuples_selected)[(*ptr_tuple_index)[imid]] = FALSE;
		return((*ptr_tuple_index)[imid]);
	}
	if (compare > 0)
	{
		imid++;
	}
	memcpy(&((*ptr_tuple_array)[stats->num_tuples]), ptr_tuple, sizeof(Tuple));
	/* Default: consider tuples that occur only once as selected */
	(*ptr_tuples_selected)[stats->num_tuples] = TRUE;
	for (t = stats->num_tuples; t >= imid; t--)
	{
		(*ptr_tuple_index)[t + 1] = (*ptr_tuple_index)[t];
	}
	(*ptr_tuple_index)[imid] = stats->num_tuples;
	stats->num_tuples++;
	return((*ptr_tuple_index)[imid]);
}

void print_labels(Stats* stats, INTEGER** labels, STRING* label_text)
{
	INTEGER l;
	INTEGER p;
	printf("labels count=%d\n", stats->num_labels);
	for(l = 0; l < stats->num_labels; l++)
	{
		printf("[%d]=%s: ", l, label_text[l]);
		for(p = 0; p < stats->num_pages; p++)
		{
			printf("%c%d", (p == 0 ? '[' : ' '), labels[l][p]);
		}
		printf("]\n");
	}
	printf("done labels\n");
}

/* prints just the array of labels */
void output_labels(Files* files, Stats* stats, STRING* label_text)
{
	INTEGER l;
	/* print the list of labels */
	for (l = 0; l < stats->num_labels; l++)
	{
		fprintf(files->output_fp, "%c \"%s\"", (l == 0 ? '[' : ','), label_text[l]);
	}
	fprintf(files->output_fp, " ]");
}

/* print the 3-tuple and the list of coefficients for each label */
void output_learner(INTEGER index, Files* files, Stats* stats, WeakLearner* weak_learner,
	Tuple* tuple_array)
{
	INTEGER i;
	INTEGER l;
	Tuple* tuple;
	printf("tuple[%d] = \"", index);
	tuple = &(tuple_array[weak_learner->tuple]);
	for(i = 0; i < (tuple->tuple_length - 1); i++)
	{
		printf("%c", tuple->tuple_strings[i] == '\0' ? ' ' : tuple->tuple_strings[i]);
	}
	printf("\"\n");
	fprintf(files->output_fp, "{ \"tuple\" : ");
	fprintf(files->output_fp, "\"");
	for(i = 0; i < (tuple->tuple_length - 1); i++)
	{
		fprintf(files->output_fp, "%c",
			tuple->tuple_strings[i] == '\0' ? ' ' : tuple->tuple_strings[i]);
	}
	fprintf(files->output_fp, "\", ");
	fprintf(files->output_fp, " \"alpha\" : %f ", weak_learner->alpha);
	fprintf(files->output_fp, ", \"c1\" : ");
	for (l = 0; l < stats->num_labels; l++)
	{
		fprintf(files->output_fp, "%c %f", (l == 0 ? '[' : ','), weak_learner->c1[l]);
	}
	fprintf(files->output_fp, "]");
	fprintf(files->output_fp, ", \"c0\" : ");
	for (l = 0; l < stats->num_labels; l++)
	{
		fprintf(files->output_fp, "%c %f", (l == 0 ? '[' : ','), weak_learner->c0[l]);
	}
	fprintf(files->output_fp, "] }\n");
}

/* Output the weak learners in json format */
void boostexter(Files* files, Stats* stats, INTEGER** labels, INTEGER** tuples,
	BOOLEAN* tuples_selected, STRING* label_text, Tuple* tuple_array)
{
	INTEGER i;
	INTEGER p;
	INTEGER l;
	INTEGER t;
	INTEGER iterations;
	REAL epsilon;
	REAL alpha;
	REAL normalization;
	REAL** dist;
	REAL weight;
	REAL* c0;
	REAL* c1;
	REAL* min_c0;
	REAL* min_c1;
	c0 = malloc(stats->num_labels * sizeof(REAL));
	c1 = malloc(stats->num_labels * sizeof(REAL));
	min_c0 = malloc(stats->num_labels * sizeof(REAL));
	min_c1 = malloc(stats->num_labels * sizeof(REAL));
	WeakLearner weak_learner;
	WeakLearner* learners;
	/* AdaBoost.MH: Select the list of weak learners and update the distribution */
	fprintf(files->output_fp, "{ ");
	fprintf(files->output_fp, "\"labels\" : ");
	output_labels(files, stats, label_text);
	fprintf(files->output_fp, ",\n");
	/* DID NOT USE: classifier_count = sum of labels in on each page line */
	/* CHECK THIS: maybe dist is nonzero for only the positive cases on the PAGE lines */
	epsilon = 1.0 / (stats->num_pages * stats->num_labels);
	/* The list of weak learners */
	learners = malloc(NUM_ITERATIONS * sizeof(WeakLearner));
	/* the weighting on each page <-> label pair: dist[p][l] = real-number */
	dist = malloc(stats->num_pages * sizeof(REAL*));
	for (p = 0; p < stats->num_pages; p++)
	{
		dist[p] = malloc(stats->num_labels * sizeof(REAL));
		for (l = 0; l < stats->num_labels; l++)
		{
			dist[p][l] = epsilon;
		}
	}
	iterations = 0;
	for (t = 0; t < stats->num_tuples; t++)
	{
		if (tuples_selected[t] == FALSE)
		{
			iterations++;
		}
	}
	printf("not selected=%d\n", iterations);
	if (iterations > NUM_ITERATIONS)
	{
		iterations = NUM_ITERATIONS;
	}
	printf("iterations=%d\n", iterations);
	fprintf(files->output_fp, "\"learners\" : [\n");
	for (i = 0; i < iterations; i++)
	{
		if (i > 0)
		{
			fprintf(files->output_fp, ", ");
		}
		get_weak_learner(stats, &weak_learner, &normalization, &alpha,
			dist, labels, tuples, tuples_selected, c0, c1, min_c0, min_c1);
		/* Ignore this normalization, normalize so distp][l] sums to 1 */
		for (p = 0; p < stats->num_pages; p++)
		{
			for (l = 0; l < stats->num_labels; l++)
			{
				if (labels[l][p] == 1)
				{
					weight = weak_learner.c1[l];
				} else {
					weight = weak_learner.c0[l];
				}
				dist[p][l] = dist[p][l] * exp(-alpha * labels[l][p] * weight);
			}
		}
		normalization = 0.0;
		for (p = 0; p < stats->num_pages; p++)
		{
			for (l = 0; l < stats->num_labels; l++)
			{
				normalization += dist[p][l];
			}
		}
		for (p = 0; p < stats->num_pages; p++)
		{
			for (l = 0; l < stats->num_labels; l++)
			{
				dist[p][l] = dist[p][l] / normalization;
			}
		}
		memcpy((void *) &learners[i], (void *) &weak_learner, sizeof(WeakLearner));
		output_learner(i, files, stats, &weak_learner, tuple_array);
	}
	fprintf(files->output_fp, "] ");
	fprintf(files->output_fp, "}\n");
}

void get_weak_learner(Stats* stats, WeakLearner* weak_learner, REAL* normalization, REAL* alpha,
	REAL** dist, INTEGER** labels, INTEGER** tuples, BOOLEAN* tuples_selected,
	REAL* c0, REAL* c1, REAL* min_c0, REAL* min_c1)
{
	INTEGER i;
	INTEGER t;
	INTEGER l;
	INTEGER p;
	REAL epsilon;
	REAL min_norm;
	BOOLEAN first;
	REAL weight_1_pos;
	REAL weight_0_pos;
	REAL weight_1_neg;
	REAL weight_0_neg;
	INTEGER min_tuple;
	/* weak learner for tuple: weak(page,label) = c1(label) if tuple is in page else c0(label) */
	epsilon = 1.0 / (stats->num_pages * stats->num_labels);
	first = TRUE;
	min_norm = 0.0;
	for (t = 0; t < stats->num_tuples; t++)
	{
		if (tuples_selected[t] == TRUE)
		{
			continue;
		}
		*normalization = 0.0;
		for (i = 0; i < stats->num_labels; i++)
		{
			c0[i] = 0.0;
			c1[i] = 0.0;
		}
		/* Let X1={x|tuples(tuple,page)=1} X0={x|tuples(tuple,page)=0} */
		for (l = 0; l < stats->num_labels; l++)
		{
			weight_1_pos = 0.0;
			weight_0_pos = 0.0;
			weight_1_neg = 0.0;
			weight_0_neg = 0.0;
			for (p = 0; p < stats->num_pages; p++)
			{
				/* weight is the weight of the documents labeled by label */
				if ((tuples[t][p] == 1) && (labels[l][p] == 1))
				{
					weight_1_pos += dist[p][l];
				}
				if ((tuples[t][p] == 0) && (labels[l][p] == 1))
				{
					weight_0_pos += dist[p][l];
				}
				if ((tuples[t][p] == 1) && (labels[l][p] == -1))
				{
					weight_1_neg += dist[p][l];
				}
				if ((tuples[t][p] == 0) && (labels[l][p] == -1))
				{
					weight_0_neg += dist[p][l];
				}
			}
			c0[l] = 0.5 * log((weight_0_pos + epsilon) / (weight_0_neg + epsilon));
			c1[l] = 0.5 * log((weight_1_pos + epsilon) / (weight_1_neg + epsilon));
			*normalization += 2.0 * sqrt(weight_0_pos * weight_0_neg) + sqrt(weight_1_pos * weight_1_neg);
		}
		/* choose page_tuple with minimum normalization(tuple) along with c0[l] and c1[l] */
		if ((first == TRUE) || (min_norm > *normalization))
		//if ((first == TRUE) || (min_norm < *normalization))
		{
			first = FALSE;
			min_norm = *normalization;
			min_tuple = t;
			memcpy(min_c0, c0, (stats->num_labels * sizeof(REAL)));
			memcpy(min_c1, c1, (stats->num_labels * sizeof(REAL)));
		}
	}
	*alpha = 1.0;
	//*alpha = min_norm;
	//*alpha = 1.0 / min_norm;
	weak_learner->tuple = min_tuple;
	weak_learner->alpha = *alpha;
	weak_learner->c0 = malloc(stats->num_labels * sizeof(REAL));
	weak_learner->c1 = malloc(stats->num_labels * sizeof(REAL));
	memcpy(weak_learner->c0, min_c0, (stats->num_labels * sizeof(REAL)));
	memcpy(weak_learner->c1, min_c1, (stats->num_labels * sizeof(REAL)));
	tuples_selected[min_tuple] = TRUE;
}

#!/usr/bin/env python

import sys
import math
import struct
import json
from collections import namedtuple

def read_model(model_name):
    with open(model_name, 'r') as input_file:
        model_string = '\n'.join(line.rstrip() for line in input_file)
    model = json.loads(model_string)
    return model

def write_model(model_name, model):
    with open(sys.argv[3], 'w') as output:
        # Start JSON output
        output.write('{ ')
        output.write('"labels" : [ {} ],\n'.format(', '.join('"{0}"'.format(l) for l in model['labels'])))
        if 'priors' in model:
            output.write('"priors" : {},\n'.format(model['priors']))
        output.write('"learners" : [\n')
        sep = ''
        for learner in model['learners']:
            if learner['tuple'] == '':
                continue
            output.write(sep + ' { ')
            output.write('"tuple" : "{}", '.format(learner['tuple']))
            output.write('"alpha" : {}, '.format(learner['alpha']))
            output.write('"c1" : {}, '.format(learner['c1']))
            output.write('"c0" : {}'.format(learner['c0']))
            output.write(' }\n')
            sep = ','
        output.write('] }\n')

def read_data_text(data_file):
    """ Read data file with lines: PAGE	URL and associated page text """
    pages = []
    url = ''
    with open(data_file, 'r') as input_file:
        for line in input_file:
            # skip comments
            if line[0] == '#':
                continue
            page_field = line.strip().split('	')
            if page_field[0] == 'PAGE':
                # Note: label_field[1] = URL for PAGE
                url = page_field[1]
                pages.append(url + '\t')
            else:
                # Separate out html: <foo>
                line = line.replace("<", " <")
                line = line.replace(">", "> ")
                # Add this line to the page on that PAGE
                pages[-1] = pages[-1] + ' ' + line
    return pages

def read_data_tuples(data_file):
    """
    Read file with lines: PAGE	URL	LABEL_1	LABEL_2	...	LABEL_n and associated text for page
    Input file is first argument on command line
    Create a list of all 3-tuples from page texts and tuples array
    Create a list of all label names and labels array
    """
    # the page for each input "PAGE": pages[p] = "string of text separated by spaces"
    pages = []
    # labels for each page: labels[l][p] = 1 if label l is a label for page p else -1
    labels = []
    # the label_text for each label
    label_text = []
    # the unique 3-tuples in all the pages: tuples[t][p] = 1 if 3-tuple t is in page p
    tuples = []
    # set tuples_selected[t] = True if tupe t has been selected as a weak learner or should be skipped
    tuples_selected = []
    # the tuple text for each tuple
    tuple_text = []
    # the list of labels for each page: page_labels[p] = [ "LABEL1", "LABEL2", ... ]
    page_labels = []
    with open(data_file, 'r') as input_file:
        for line in input_file:
            line = line.strip()
            label_field = line.split('	')
            if label_field[0] == 'PAGE':
                # Read and parse PAGE line into page_labels
                # Note: label_field[1] = URL for PAGE
                page_labels.append(label_field[2:])
                pages.append('')
            else:
                # Separate out html: <foo>
                line = line.replace("<", " <")
                line = line.replace(">", "> ")
                # Add this line to the page on that PAGE
                pages[-1] = pages[-1] + ' ' + line
    print 'read {} pages'.format(len(pages))
    create_tuples(pages, tuples, tuples_selected, tuple_text)
    print 'created {} tuples'.format(len(tuples))
    create_labels(page_labels, label_text, labels)
    return pages, labels, label_text, tuples, tuples_selected, tuple_text

def read_data_labels(data_file_name, model):
    """ Read data file with lines: PAGE	URL and associated page text """
    label_names = model['labels']
    label_dict = {}
    for l in range(len(label_names)):
        label_dict[label_names[l]] = l
    page_text = []
    page_labels = []
    page_num_labels = []
    with open(data_file_name, 'r') as input_file:
        for line in input_file:
            # skip comments
            if line[0] == '#':
                continue
            line = line.strip()
            page_field = line.split('	')
            if page_field[0] == 'PAGE':
                # Note: label_field[1] = URL for PAGE
                url = page_field[1]
                page_text.append("")
                labels_flag = [0 for l in range(len(label_names))]
                for label in page_field[2:]:
                    if label in label_dict:
                        labels_flag[label_dict[label]] = 1
                page_labels.append(labels_flag)
                page_num_labels.append(len(page_field[2:]))
            else:
                line = ' '.join(line.split())
                # Separate out html: <foo>
                line = line.replace("<", " <")
                line = line.replace(">", "> ")
                # Add this line to the page on that PAGE
                page_text[-1] = page_text[-1] + ' ' + line
    num_pages = len(page_text)
    num_tuples = len(model['learners'])
    page_tuples = []
    for p in range(num_pages):
        page_tuples.append([0 for t in range(num_tuples)])
        for t in range(num_tuples):
            n_tuple = model['learners'][t]['tuple']
            if n_tuple in page_text[p]:
                page_tuples[p][t] = 1
    return page_tuples, page_labels, page_num_labels

def create_tuples(pages, tuples, tuples_selected, tuple_text):
    """ Create dictionary of 3-tuples in all of the pages """
    # the list of unique tuples: labels["a b c"] = t
    tuple_list = {}
    num_pages = len(pages)
    num_tuples = 0
    for p in range(len(pages)):
        words = pages[p].split()
        for i in range(len(words) - 2):
            page_tuple = words[i] + ' ' + words[i + 1] + ' ' + words[i + 2]
            if page_tuple in tuple_list:
                t = tuple_list[page_tuple]
                tuples[t][p] = 1
            else:
                tuple_list[page_tuple] = num_tuples
                tuples.append([0 for i in range(num_pages)])
                tuples_selected.append(False)
                tuples[num_tuples][p] = 1
                num_tuples += 1
    tuple_text.extend([0 for i in range(num_tuples)])
    for key, value in tuple_list.iteritems():
        tuple_text[value] = key

def create_labels(page_labels, label_text, labels):
    """ Create array of labels(page, label) = 1 if page has label -1 if not """
    # the list of unique labels: labels["LABEL"] = l
    label_list = {}
    # Note: the size of page_labels is the number of pages
    num_pages = len(page_labels)
    num_labels = 0
    # Count the number of unique labels
    for p in range(num_pages):
        for l in range(len(page_labels[p])):
            label = page_labels[p][l]
            if label not in label_list:
                label_list[label] = num_labels
                labels.append([-1 for i in range(num_pages)])
                num_labels += 1
    for p in range(num_pages):
        for l in range(len(page_labels[p])):
            label = page_labels[p][l]
            labels[label_list[label]][p] = 1
    label_text.extend([0 for i in range(num_labels)])
    for key, value in label_list.iteritems():
        label_text[value] = key

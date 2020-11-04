#!/usr/bin/env python

# input the pages, with labels
# P(l) = prior probability of a label is # pages with label l / #pages total
# for a given tuple t and label p
# P(t|l) = #pages with label l and tuple t / #pages with label l
# given an unknown page p
# P(l|p) = P(l) * PRODUCT of (P(t|l) if t in p or (1 - P(t|l) if t not in p)
# Order the labels by P(l|p)

import sys
import math
import struct
import json
from collections import namedtuple
from readutils import read_model, read_data_tuples, write_model

NUM_ITERATIONS = 3
WeakLearner = namedtuple('WeakLearner', ['tuple', 'alpha', 'c1', 'c0'])

def main():
    """
    Command Line Inputs:
    Input: Data file with lines: PAGE	URL	LABEL_1	LABEL_2	...	LABEL_n and associated text for page
    Input: Model file with labels and array of weak learners
    Output: New Model file with labels and array of weak learners with naive bayesian weights
    """
    pages, labels, label_text, tuples, tuples_selected, tuple_text = read_data_tuples(sys.argv[1])
    model = read_model(sys.argv[2])
    print 'model has {} labels and {} tuples'.format(len(model['labels']), len(model['learners']))
    naivebayes(model, labels, tuples, tuples_selected, label_text, tuple_text)
    write_model(sys.argv[3], model)

def naivebayes(model, labels, tuples, tuples_selected, label_text, tuple_text):
    # convert weights to bayesian expected values
    num_labels = len(labels)
    num_pages = len(labels[0])
    model['priors'] = [0.0] * num_labels
    page_count = [1.0] * num_labels
    for l in range(num_labels):
        page_count[l] = 1.0
        for p in range(num_pages):
            if labels[l][p] == 1:
                page_count[l] += 1.0
        if page_count[l] < (num_pages + 1.0):
            model['priors'][l] = page_count[l] / (num_pages + 1.0)
        else:
            model['priors'][l] = (page_count[l] - 1.0) / (num_pages + 1.0)
    for learner in model['learners']:
        n_tuple = learner['tuple']
        learner['alpha'] = 1.0
        learner['c0'] = [0.0] * num_labels
        learner['c1'] = [0.0] * num_labels
        if n_tuple not in tuple_text:
            learner['tuple'] = ''
            continue
        # P(t|l) = #pages with label l and tuple t / #pages with label l
        t = tuple_text.index(n_tuple)
        for p in range(num_pages):
            if tuples[t][p] == 1:
                for l in range(num_labels):
                    if labels[l][p] == 1:
                        learner['c1'][l] += 1.0
        for l in range(num_labels):
            cnt = learner['c1'][l]
            page = page_count[l]
            if learner['c1'][l] < page_count[l]:
                learner['c1'][l] = (learner['c1'][l] + 1.0) / (page_count[l] + 1.0)
            else:
                learner['c1'][l] = (learner['c1'][l] - 1.0) / page_count[l]
            if learner['c1'][l] <= 0.0:
                print 'FATAL ERROR learner[c1][{}]={} <= 0.0 cnt={} page={}'.format(l, learner['c1'][l], cnt, page)

if __name__ == '__main__':
    main()

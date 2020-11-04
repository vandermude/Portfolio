#!/usr/bin/env python

# normalize weights on boostexter models

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
    Output: New Model file with labels and array of weak learners with normalized weights
    """
    pages, labels, label_text, tuples, tuples_selected, tuple_text = read_data_tuples(sys.argv[1])
    model = read_model(sys.argv[2])
    print 'model has {} labels and {} tuples'.format(len(model['labels']), len(model['learners']))
    # for l in range(len(labels)):
        # print '[{}]={}: [{}]'.format(l, label_text[l], \
        #    ' '.join(str(labels[l][p]) for p in range(len(labels[0]))))
    cntnorm(model, labels, tuples, label_text, tuple_text)
    write_model(sys.argv[3], model)

def cntnorm(model, labels, tuples, label_text, tuple_text):
    # convert weights to counts of tuples for a given label
    for learner in model['learners']:
        n_tuple = learner['tuple']
        if n_tuple not in tuple_text:
            learner['tuple'] = ''
            continue
        learner['c0'] = [0.0] * len(learner['c0'])
        learner['c1'] = [0.0] * len(learner['c0'])
        # NOTE: if n_tuple text gives an error, delete it (especially strings with '\' in it
        t = tuple_text.index(n_tuple)
        for p in range(len(labels[0])):
            if tuples[t][p] == 1:
                for l in range(len(learner['c1'])):
                    if labels[l][p] == 1:
                        learner['c1'][l] += 1.0
    for l in range(len(learner['c1'])):
        total = sum([learner['c1'][l] for learner in model['learners']])
        if total > 0:
            for learner in model['learners']:
                learner['c1'][l] = learner['c1'][l] / total

if __name__ == '__main__':
    main()

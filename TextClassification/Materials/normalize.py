#!/usr/bin/env python

"""
Game plan
for each label do:
    for each page do:
        look only at the pages that have a positive scode:
            compute: score_right score_wrong arrays
            given all of the scores, find the score where there is the minimum count of FP/FN
            this score will be the divisor, so that the cutoff will be 1.0
"""

import sys
import math
import struct
import json
from collections import namedtuple
from copy import deepcopy
from readutils import read_model, write_model, read_data_labels
from classifier import classifier_tuples
from matthews import score_errors, score_matthews, get_page_count, weighted_average

#NUM_ITERATIONS = 10
TVDBG_P = 0
TVDBG_L = 0
NUM_ITERATIONS = 3
WeakLearner = namedtuple('WeakLearner', ['tuple', 'alpha', 'c1', 'c0'])

def main():
    """
    Command Line Inputs:
    Data file with lines: PAGE	URL	LABEL_1	LABEL_2	...	LABEL_n and associated text for page
    Input Model file with labels and array of weak learners
    Output Model file with correction factors
    Print the matthews correlation coefficient for each label
    """
    model = read_model(sys.argv[2])
    print 'model has {} labels and {} tuples'.format(len(model['labels']), len(model['learners']))
    page_tuples, page_labels, page_num_labels = read_data_labels(sys.argv[1], model)
    print 'read {} pages'.format(len(page_labels))
    evaluate(model, page_tuples, page_labels, page_num_labels)
    model = normalize(model, page_tuples, page_labels, page_num_labels)
    write_model(sys.argv[3], model)
    evaluate(model, page_tuples, page_labels, page_num_labels)

def evaluate(model, page_tuples, page_labels, page_num_labels):
    TP, TN, FP, FN = score_errors(model, page_tuples, page_labels, page_num_labels, True, TVDBG_P, TVDBG_L)
    matthews = score_matthews(TP, TN, FP, FN, TVDBG_P, TVDBG_L)
    page_total, page_count = get_page_count(page_labels)
    wgt_avg = weighted_average(page_total, page_count, matthews)
    print 'weighted average = {} matthews={}'.format(wgt_avg, matthews)

def normalize(model, page_tuples, page_labels, page_num_labels):
    num_pages = len(page_labels)
    num_labels = len(page_labels[0])
    num_tuples = len(page_tuples[0])
    page_total, page_count = get_page_count(page_labels)
    # this is very inefficient
    score_right = [[] for l in range(num_labels)]
    score_wrong = [[] for l in range(num_labels)]
    score_both = [[] for l in range(num_labels)]
    for p in range(num_pages):
        # scores = classifier_tuples(model, page_tuples, p)
        scores = classifier_tuples(model, page_tuples, p, TVDBG_P, TVDBG_L)
        # if p == 0: print 'tvdbg before p={} scores={}'.format(p, scores)
        for l in range(num_labels):
            if scores[l] > 0:
                score_both[l].append(scores[l])
                if page_labels[p][l] == 1:
                    score_right[l].append(scores[l])
                else:
                    score_wrong[l].append(scores[l])
    # need to remove all labels with score_both[l] <= 1
    for l in range(num_labels):
        min_errs = num_pages
        if len(score_both[l]) <= 1:
            print 'ERROR: set factor to 0 since only {} positive scores for label {} = {}'.format(len(score_both[l]), l, model['labels'][l])
            factor = 0.0
        else:
            for i in range(1, len(score_both[l])):
                score_both[l] = sorted(score_both[l])
                # a false negative at level score_both[p] is a value score_right[p] < score_both[p]
                FN = 0
                for j in range(len(score_right[l])):
                    if score_right[l][j] < score_both[l][i]:
                        FN += 1
                # a false positive at level score_both[p] is a value score_wrong[p] >= score_both[p]
                FP = 0
                for j in range(len(score_wrong[l])):
                    if score_wrong[l][j] >= score_both[l][i]:
                        FP += 1
                if min_errs > (FN + FP):
                    min_errs = FN + FP
                    factor = (score_both[l][i - 1] + score_both[l][i]) / 2.0
        for t in range(num_tuples):
            learner = model['learners'][t]
            if factor == 0.0:
                learner['c1'][l] = 0.0
                learner['c0'][l] = 0.0
            else:
                learner['c1'][l] = learner['c1'][l] / factor
                learner['c0'][l] = learner['c0'][l] / factor
    for p in range(num_pages):
        # scores = classifier_tuples(model, page_tuples, p)
        scores = classifier_tuples(model, page_tuples, p, TVDBG_P, TVDBG_L)
        # if p == 0: print 'tvdbg after p={} scores={}'.format(p, scores)
    return model

if __name__ == '__main__':
    main()

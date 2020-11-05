#!/usr/bin/env python

import sys
import math
import struct
import json
from collections import namedtuple
from readutils import read_data_tuples

NUM_ITERATIONS = 2000
WeakLearner = namedtuple('WeakLearner', ['tuple', 'alpha', 'c1', 'c0'])

def main():
    """
    Command Line Inputs:
    Data file with lines: PAGE	URL	LABEL_1	LABEL_2	...	LABEL_n and associated text for page
    Model file with labels and array of weak learners
    """
    pages, labels, label_text, tuples, tuples_selected, tuple_text = read_data_tuples(sys.argv[1])
    tuples_available = remove_singletons(tuples, tuples_selected)
    # for l in range(len(labels)):
        # print '[{}]={}: [{}]'.format(l, label_text[l], \
        #    ' '.join(str(labels[l][p]) for p in range(len(labels[0]))))
    boostexter(labels, tuples, tuples_available, tuples_selected, label_text, tuple_text)

def remove_singletons(tuples, tuples_selected):
    """ mark singletons as done """
    tuples_available = 0
    for t in range(len(tuples)):
        if sum(tuples[t]) <= 1:
            tuples_selected[t] = True
        else:
            tuples_available += 1
    return tuples_available

def boostexter(labels, tuples, tuples_available, tuples_selected, label_text, tuple_text):
    """ AdaBoost.MH: Select the list of weak learners and update the distribution """
    with open(sys.argv[2], 'w') as output:
        # Start JSON output
        output.write('{ ')
        output.write('"labels" : [ {} ],\n'.format(', '.join('"{0}"'.format(l) for l in label_text)))
        output.write('"learners" : [\n')
        num_pages = len(labels[0])
        num_labels = len(label_text)
        # DID NOT USE: classifier_count = sum of labels in on each page line
        # CHECK THIS: maybe dist is nonzero for only the positive cases on the PAGE lines
        epsilon = 1.0 / (num_pages * num_labels)
        # the weighting on each page <-> label pair: dist[p][l] = real-number
        dist = [[epsilon for i in range(num_labels)] for j in range(num_pages)]
        for i in range(NUM_ITERATIONS):
            if tuples_available == 0:
                break;
            else:
                if i != 0:
                    output.write(',\n')
            weak_learner, normalization, alpha, tuples_available = \
                get_weak_learner(dist, labels, tuples, tuples_available, tuples_selected)
            for p in range(num_pages):
                for l in range(num_labels):
                    if labels[l][p] == 1:
                        weight = weak_learner.c1[l]
                    else:
                        weight = weak_learner.c0[l]
                    dist[p][l] = dist[p][l] * math.exp(-alpha * labels[l][p] * weight) / normalization
            # print "tuple : '{}'".format(tuple_text[weak_learner.tuple])
            output.write('{ ')
            output.write('"tuple" : "{}", "alpha" : {}, "c1" : {}, "c0" : {}'. \
                format(tuple_text[weak_learner.tuple], weak_learner.alpha, \
                    weak_learner.c1, weak_learner.c0))
            output.write("normalization = {}".format(normalization))
            output.write(' }')
        output.write('\n] }\n')

def get_weak_learner(dist, labels, tuples, tuples_available, tuples_selected):
    # weak learner for tuple: weak(page,label) = c1(label) if tuple is in page else c0(label)
    num_labels = len(labels)
    num_pages = len(labels[0])
    num_tuples = len(tuples)
    epsilon = 1.0 / (num_pages * num_labels)
    min_norm = None
    for t in range(num_tuples):
        if tuples_selected[t]:
            continue
        normalization = 0.0
        c0 = [0.0 for i in range(num_labels)]
        c1 = [0.0 for i in range(num_labels)]
        # Let X1={x|tuples(tuple,page)=1} X0={x|tuples(tuple,page)=0}
        for l in range(num_labels):
            weight_1_pos = 0.0
            weight_0_pos = 0.0
            weight_1_neg = 0.0
            weight_0_neg = 0.0
            for p in range(num_pages):
                # print "tuples[{}][{}] = {} labels[{}][{}] = {}".format(t, p, tuples[t][p], l, p, labels[l][p])
                # weight is the weight of the documents labeled by label
                if tuples[t][p] == 1 and labels[l][p] == 1:
                    weight_1_pos += dist[p][l]
                if tuples[t][p] == 0 and labels[l][p] == 1:
                    weight_0_pos += dist[p][l]
                if tuples[t][p] == 1 and labels[l][p] == -1:
                    weight_1_neg += dist[p][l]
                if tuples[t][p] == 0 and labels[l][p] == -1:
                    weight_0_neg += dist[p][l]
            # print"weight_1_pos={} weight_0_pos={} weight_1_neg={} weight_0_neg={}".format(weight_1_pos, weight_0_pos, weight_1_neg, weight_0_neg);
            c0[l] = 0.5 * math.log((weight_0_pos + epsilon) / (weight_0_neg + epsilon))
            c1[l] = 0.5 * math.log((weight_1_pos + epsilon) / (weight_1_neg + epsilon))
            normalization += 2.0 * math.sqrt(weight_0_pos * weight_0_neg) + \
                math.sqrt(weight_1_pos * weight_1_neg)
        # choose page_tuple with minimum normalization(tuple) along with c0[l] and c1[l]
        if (min_norm == None) or (min_norm > normalization):
            min_norm = normalization
            min_tuple = t
            min_c0 = c0
            min_c1 = c1
    alpha = 1.0
    tuples_selected[min_tuple] = True
    tuples_available -= 1
    print "min_norm = {}".format(min_norm)
    return (WeakLearner(min_tuple, alpha, min_c1, min_c0), min_norm, alpha, tuples_available)

if __name__ == '__main__':
    main()

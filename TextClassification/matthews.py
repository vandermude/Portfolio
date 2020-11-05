#!/usr/bin/env python

import math
from classifier import classifier_tuples

def get_page_count(page_labels):
    num_pages = len(page_labels)
    num_labels = len(page_labels[0])
    page_count = [0 for i in range(num_labels)]
    page_total = 0
    for l in range(num_labels):
        for p in range(num_pages):
            page_count[l] += page_labels[p][l]
            page_total += page_labels[p][l]
    return page_total, page_count

def weighted_average(page_total, page_count, matthews):
    wgt_avg = 0
    for l in range(len(page_count)):
        wgt_avg += matthews[l] * page_count[l]
    wgt_avg = wgt_avg / page_total
    return wgt_avg

def score_errors(model, page_tuples, page_labels, page_num_labels, cutoff):
    num_pages = len(page_labels)
    num_labels = len(page_labels[0])
    # weighted counts: TP = true positive, etc.
    TP = [0.0 for i in range(num_labels)]
    FP = [0.0 for i in range(num_labels)]
    FN = [0.0 for i in range(num_labels)]
    TN = [0.0 for i in range(num_labels)]
    matthews = [0 for i in range(num_labels)]
    for p in range(num_pages):
        model_score = classifier_tuples(model, page_tuples, p)
        if cutoff:
            sort_score = sorted(model_score, key=float, reverse=True)
            cutoff = sort_score[page_num_labels[p] - 1]
            for l in range(len(model_score)):
                if model_score[l] < cutoff:
                    model_score[l] = -1
        # weight of highest and lowest score
        weight_max = max(model_score)
        # score how right or wrong the classifier is
        # instead of score being compared to zero, you might want to
        # use (weight_max + weight_min) / 2
        for l in range(num_labels):
            score = model_score[l]
            # TP = true positive
            if page_labels[p][l] == 1 and score > 0:
                TP[l] += score / weight_max
            # FP = false positive
            if page_labels[p][l] == 0 and score > 0:
                FP[l] += score / weight_max
            # TN = true negative
            if page_labels[p][l] == 1 and score <= 0:
                FN[l] += 1.0
            # FN = false negative
            if page_labels[p][l] == 0 and score <= 0:
                TN[l] += 1.0
    # once all the pages are processed,
    # normalize TP, TN, FP, FN to 1
    for l in range(num_labels):
        den = TP[l] + TN[l] + FP[l] + FN[l]
        # print 'den[{}]({}) = TP[l]({}) + TN[l]({}) + FP[l]({}) + FN[l]({})'.format(l, den, TP[l], TN[l], FP[l], FN[l])
        TP[l] = TP[l] / den
        TN[l] = TN[l] / den
        FP[l] = FP[l] / den
        FN[l] = FN[l] / den
    return TP, TN, FP, FN

def score_matthews(TP, TN, FP, FN):
    matthews = [0 for l in range(len(TP))]
    # compute the matthews correlation coefficient
    for l in range(len(TP)):
        den = math.sqrt((TP[l] + FP[l]) * (TP[l] + FN[l]) * (TN[l] + FP[l]) * (TN[l] + FN[l]))
        if den == 0.0:
            den = 1.0
        matthews[l] = ((TP[l] * TN[l]) - (FP[l] * FN[l])) /  den
    return matthews

def score_positive(TP, TN, FP, FN):
    score = [0 for l in range(len(TP))]
    # compute the score: TP / (TP + FP)
    for l in range(len(TP)):
        if TP[l] == 0:
            score[l] = 0
        else:
            score[l] = TP[l] / (TP[l] + FP[l])
    return score

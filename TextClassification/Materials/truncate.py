#!/usr/bin/env python

import sys

def main():
    filestem = sys.argv[1]
    filemodel = filestem + '.model'
    tuple_cnt = 0
    with open(filemodel, 'r') as modelfile:
        for line in modelfile:
            tuple_cnt += 1
    tuple_cnt -= 3
    # create 20 models if tuple_cnt >= 200
    # create models of size 10 if tuple_cnt < 200
    if tuple_cnt >= 200:
        line_incr = tuple_cnt / 20
        line_step = 20
    else:
        line_incr = 10
        line_step = tuple_cnt / line_incr
    # this is simple-minded: read and write line_step times
    # no need to be fancy
    file_end = 2
    for i in range(line_step):
        file_end += line_incr
        if i == (line_step - 1):
            file_end = tuple_cnt + 2
        if i < 10:
            file_trunc = filestem + '.0' + str(i) + '.model'
        else:
            file_trunc = filestem + '.' + str(i) + '.model'
        read_cnt = 0
        with open(filemodel, 'r') as modelfile:
            with open(file_trunc, 'w') as trunc_file:
                for line in modelfile:
                    read_cnt += 1
                    if read_cnt <= file_end:
                        trunc_file.write(line)
                    else:
                        break
                trunc_file.write('] }\n')

if __name__ == '__main__':
    main()


#!/usr/bin/env python

import sys

def main():
    filestem = sys.argv[1]
    file_dat = filestem + '.dat'
    # this is simple-minded: read and write 5 times
    # no need to be fancy
    for i in range(5):
        filetest = filestem + '_' + str(i) + '.test'
        filetrain = filestem + '_' + str(i) + '.train'
        page_no = -1
        testflag = False
        with open(file_dat, 'r') as input_file:
            with open(filetest, 'w') as test_file:
                with open(filetrain, 'w') as train_file:
                    for line in input_file:
                        if line[0] == '#':
                            continue
                        line_strip = line.strip()
                        label_field = line_strip.split('	')
                        if label_field[0] == 'PAGE':
                            page_no += 1
                            if (page_no % 5) == i:
                                testflag = True
                            else:
                                testflag = False
                        if testflag:
                            test_file.write(line)
                        else:
                            train_file.write(line)

if __name__ == '__main__':
    main()


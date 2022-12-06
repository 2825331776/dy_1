//
// Created by stefa on 2022/12/6.
//

#ifndef DYTPIRCAMERA_DYTJPGHELPER_H
#define DYTPIRCAMERA_DYTJPGHELPER_H
#include "stdio.h"

class DYTJPGHelper{

private:


public:
    bool JPG_Is_DYTFormat(const char * jpgPath);

    int Search_JPG_FFE0_Index(FILE *file);


};

#endif //DYTPIRCAMERA_DYTJPGHELPER_H

/*
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information
 *
 * ClassifyTrajectory.cpp
 *
 */

#include <stdlib.h>
#include <cstring>
#include <ctype.h>
#include <vector>
#include "ANNx.h"


#include "Utils.h"
#include "BuildTree.h"
#include "TDEModel.h"
#include "Classifier.h"
#include "ClassifyTrajectory.h"

#define STR_LEN 100

Classifier *classifier;

void loadModels(const char* ms_file, int numNeighbours, int matchSteps) {
	NamedModel *model;
    FILE* models_file;
    FILE* model_file;
    char buf[STR_LEN+1];
    int n = STR_LEN;
    std::vector<NamedModel*> *models = new std::vector<NamedModel*>();

    ////HS_2("Using models from: %s", ms_file);

    // Read the models.ini file.
    models_file = fopen(ms_file, "r"); // models_file.open(ms_file);

    while(fgets(buf, STR_LEN + 1, models_file) != NULL) {
    	if (strlen(buf) > 0) {
    		check_alloc(model=(NamedModel*)calloc(1,sizeof(NamedModel)));
    		check_alloc(model->name=(char*)calloc(strlen(buf),sizeof(char)));
    		strncpy(model->name,buf, strlen(buf));
    		model->name[strlen(buf)-1] = '\0';
    		model_file = fopen(model->name, "r");
    		if (model_file == NULL) {
    			return;
    		}
    		model->model = new TDEModel(model_file);
    		fclose(model_file);
    		models->push_back(model);
    	}
    }
    classifier = new Classifier(models, (uint)numNeighbours, (uint)matchSteps);

}

void cleanUpModels() {
	delete classifier;
}

ANNcoord* classifySample() {
	return NULL;
}


void classifyTrajectory(const char* in_file, const char* out_file) {
	Settings settings = { ULONG_MAX, 0, 0xff, 1, 1, 2, 2, NULL, NULL, NULL, 0 };
    FILE *fout;
    ANNcoord** data; // one double* for each model.
    ANNcoord *proj;
    int *tlengths, tlength;
    char stin=0;
    int i;

    settings.infile=(char*)in_file;
    check_alloc(settings.outfile=(char*)calloc(strlen(settings.infile)+5,sizeof(char)));
    strcpy(settings.outfile,settings.infile);
    strcat(settings.outfile,".dmp");

    std::vector<NamedModel*> *models = classifier->models;
    int numModels = models->size();
    data = new ANNcoord*[numModels];
    tlengths = new int[numModels];
    for (i = 0; i < numModels; i++) {
        settings.embdim = (*models)[i]->model->getEmbDim();
        settings.delay = (*models)[i]->model->getDelay();
        get_embedding(&settings, data[i], tlengths[i]);
        if ((*models)[i]->model->getUsePCA()) {
        	settings.pcaembdim = (*models)[i]->model->getPCAEmbDim();
        	proj = (*models)[i]->model->projectData(data[i],tlengths[i],settings.embdim);
        	delete [] data[i];
        	data[i] = proj;
        }
        else {
        	settings.pcaembdim = settings.embdim;
        }
    }
    // Since the projected data might have different lengths under different models,
    // we just use the minimum so that all points are compared under all models.
    tlength = tlengths[0];
    for (i = 1; i < numModels; i++) {
    	if (tlengths[i] < tlength) tlength = tlengths[i];
    }
    ////_android_log_write(ANDROID_LOG_DEBUG, //HS_TAG, "Data loaded. Classifying...");
    fout = fopen(out_file, "w");
    classifier->classifyAndSave(data, tlength, fout);
    fclose(fout);
    for (i = 0; i < numModels; i++) {
    	delete [] data[i];
    }
    delete [] data;
    delete [] tlengths;

    if (settings.column != NULL) free(settings.column);
    if (settings.outfile != NULL) free(settings.outfile);
}

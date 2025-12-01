package org.example.model;

class ExperimentResult {
    String fileName;
    int dataSize;
    int numWorkers;
    double timeSeconds;
    int arcsProcessed;

    ExperimentResult(String fileName, int dataSize, int numWorkers,
                     double timeSeconds, int arcsProcessed) {
        this.fileName = fileName;
        this.dataSize = dataSize;
        this.numWorkers = numWorkers;
        this.timeSeconds = timeSeconds;
        this.arcsProcessed = arcsProcessed;
    }

    // Getters
    public String getFileName() {
        return fileName;
    }

    public int getDataSize() {
        return dataSize;
    }

    public int getNumWorkers() {
        return numWorkers;
    }

    public double getTimeSeconds() {
        return timeSeconds;
    }

    public int getArcsProcessed() {
        return arcsProcessed;
    }

    @Override
    public String toString() {
        return String.format("ExperimentResult{file='%s', events=%,d, workers=%d, time=%.2fs, arcs=%d}",
                fileName, dataSize, numWorkers, timeSeconds, arcsProcessed);
    }
}
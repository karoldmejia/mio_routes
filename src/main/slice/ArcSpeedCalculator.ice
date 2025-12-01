module MioGraph {

    // Estructura para representar un datagrama GPS
    struct GpsEvent {
        int eventType;
        string date;
        int stopId;
        int odometer;
        double latitude;
        double longitude;
        int lineId;
        int tripId;
        string datagramDate;
        int busId;
    };

    // Secuencia de eventos GPS
    sequence<GpsEvent> GpsEventSeq;

    // Resultado del cálculo de velocidad para un arco
    struct ArcSpeed {
        int fromStopId;
        int toStopId;
        int routeId;
        int orientation;
        double averageSpeed;  // km/h
        int sampleCount;
        long processingTimeMs;
    };

    // Secuencia de resultados
    sequence<ArcSpeed> ArcSpeedSeq;

    // Estadísticas de procesamiento
    struct ProcessingStats {
        int nodeId;
        int eventsProcessed;
        int arcsCalculated;
        long totalTimeMs;
        string nodeName;
    };

    sequence<ProcessingStats> ProcessingStatsSeq;

    // Interface para el worker (nodo de procesamiento)
    interface SpeedCalculatorWorker {
        // Procesa un lote de eventos y retorna velocidades calculadas
        ArcSpeedSeq processEventBatch(GpsEventSeq events, int batchId);

        // Obtiene estadísticas del worker
        ProcessingStats getStats();

        // Reinicia estadísticas
        void resetStats();

        // Health check
        bool ping();
    };

    // Interface para el coordinador (master)
    interface SpeedCalculatorMaster {
        // Registra un worker en el sistema
        void registerWorker(string workerProxy, int nodeId);

        // Desregistra un worker
        void unregisterWorker(int nodeId);

        // Inicia el procesamiento de un archivo
        void processFile(string filePath);

        // Obtiene resultados agregados
        ArcSpeedSeq getAggregatedResults();

        // Obtiene estadísticas de todos los workers
        ProcessingStatsSeq getAllStats();

        // Obtiene el tiempo total de procesamiento
        long getTotalProcessingTime();
    };
};
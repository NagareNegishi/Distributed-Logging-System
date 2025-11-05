# Distributed Logging System

An enterprise logging infrastructure built with Java, combining REST APIs, Log4j extensions, and database persistence.

## Overview

A production-ready logging service that provides:
- RESTful API for log storage and retrieval
- Multiple export formats (CSV, HTML, Excel)
- Log filtering and querying capabilities
- Command-line client for data export

## Tech Stack

- Java 17
- Jakarta Servlets + Jetty
- Maven
- JSON processing
- Apache POI (Excel generation)

## Quick Start
```bash
# Build
mvn clean package

# Start server
mvn jetty:run

# Stop server
mvn jetty:stop
```

**Base URL:** `http://localhost:8080/logstore`

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/logs` | Store log event |
| GET | `/logs?limit=N&level=LEVEL` | Retrieve logs |
| DELETE | `/logs` | Clear all logs |
| GET | `/stats/csv` | Export stats as CSV |
| GET | `/stats/html` | Export stats as HTML |
| GET | `/stats/excel` | Export stats as Excel |

## CLI Client

[//]: # (```bash)

[//]: # (# Export to CSV)

[//]: # (java -cp target/classes client.io.github.nagare.logging.Client csv logs.csv)

[//]: # ()
[//]: # (# Export to Excel)

[//]: # (java -cp target/classes client.io.github.nagare.logging.Client excel logs.xlsx)

[//]: # (```)

## Project Status

🚧 **Under Active Development**

This project is being enhanced with:
- Database persistence layer (JDBC)
- Custom Log4j appenders
- Remote HTTP logging
- JMX monitoring
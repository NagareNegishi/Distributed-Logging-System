# Distributed Logging System

Enterprise logging infrastructure with remote HTTP appender, REST API, and database persistence.


## Features

- **Remote HTTP Logging**: Custom Log4j appender sends logs via HTTP POST
- **JMX Monitoring**: Track appender metrics (success/failure counts) via MBean
- **Database Persistence**: Hibernate/JPA with H2 in-memory (default) or external DB
- **RESTful API**: Store, retrieve, filter, delete, and export logs
- **Multiple Export Formats**: CSV, HTML, Excel
- **Transaction Management**: ACID compliance for log storage


## Tech Stack

- Java 17
- Jakarta Servlets + Jetty
- Maven
- Hibernate/JPA
- H2 Database (configurable)
- Apache Log4j
- Jackson (JSON)
- Apache POI (Excel)


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
| POST | `/logs` | Store log event (JSON) |
| GET | `/logs?limit=N&level=LEVEL` | Retrieve filtered logs |
| DELETE | `/logs` | Clear all logs |
| GET | `/stats/csv` | Export statistics as CSV |
| GET | `/stats/html` | Export statistics as HTML |
| GET | `/stats/excel` | Export statistics as Excel |


### Log Levels
`TRACE` | `DEBUG` | `INFO` | `WARN` | `ERROR` | `FATAL`

Filter parameters: `ALL` (show all) | `OFF` (show none)

## Configuration

### HTTP Appender (log4j.properties)
```properties
log4j.appender.http=io.github.nagare.logging.log4j.HttpAppender
log4j.appender.http.url=http://localhost:8080/logstore/logs
```

### Database
- **Default**: H2 in-memory (no setup required, data cleared on shutdown)
- **External**: Create `config.properties` in `src/main/resources/`:
```properties
db.url=jdbc:your-database-url
db.user=your-username
db.password=your-password
db.driver=your.jdbc.Driver  # Optional, auto-detected if omitted
```

**Examples:**
```properties
# PostgreSQL (Supabase)
db.url=jdbc:postgresql://host:5432/database
db.driver=org.postgresql.Driver

# MySQL
db.url=jdbc:mysql://host:3306/database
db.driver=com.mysql.cj.jdbc.Driver

# MariaDB
db.url=jdbc:mariadb://host:3306/database
db.driver=org.mariadb.jdbc.Driver
```
  - Supports any JDBC-compatible database (Tested with Supabase PostgreSQL)
  - Requires appropriate JDBC driver in dependencies


### JMX Monitoring
Monitor appender metrics via JConsole or VisualVM:

**MBean Name:** `io.github.nagare.logging.log4j:type=HttpAppender,name=HttpAppenderMBean-N`  
(where N is the instance number)

**Available Metrics:**
- `successCount` - Number of successful log transmissions
- `failureCount` - Number of failed log transmissions
- `url` - Current target endpoint


## Log Event Format

```json
{
  "id": "uuid-generated-if-not-provided",
  "message": "Log message",
  "timestamp": "2024-12-12T10:15:30Z",
  "thread": "main",
  "logger": "com.example.MyClass",
  "level": "INFO",
  "errorDetails": "Optional stack trace"
}
```

## Architecture

```
Application → HttpAppender → LogsServlet → LogEventRepository → Database
                    ↓
               JMX MBean (monitoring)
```

## Development Notes

- Logs can be sent via HttpAppender or direct HTTP POST
- UUID auto-generation for log events without ID
- Duplicate prevention by ID
- ISO-8601 timestamp validation
- Thread-safe transaction management
- Graceful failure handling (logging errors don't crash app)

# IE-Module

A module for automated **Import** and **Export** of data.  
This module is built on top of **Apache POI** and is fully extendable.  
It supports multiple database types and output formats such as **Excel**, **CSV**, and more.

## ✨ Current Features

- ✅ **Excel Processor (Implemented)**
  - Export data to Excel (.xlsx) format
  - Import data from Excel files
  - Automatic mapping of database entities to Excel rows and columns
  - Support for custom column ordering and formatting
  - Ability to define multiple sheets and specify which fields appear in which sheet
  - Ability to setup links in cells
  - Automatic header definition

- ✅ **Database-Agnostic Design**
  - Works with any database supported by JPA/Hibernate (Implemented)
  - Extendable to support NoSQL sources
  - Ability to handle lazy fetches and update sessions

- ✅ **Extensible Architecture**
  - Simple `Processor` interface to implement custom input/output formats
  - **Flexible Data Mapping**: map entity fields to DTOs or transform field values (e.g., convert dates to strings, select only specific fields)
  - **Error-tolerant processing**: can handle errors gracefully, skip invalid records, and continue processing

- ✅ **Error Handling & Logging**
  - Captures validation errors during import
  - Provides detailed logs for debugging failed rows

- ✅ **Batch Processing**
  - Handles large datasets efficiently with batching support
    
## How to Use
### 1. Add maven dependency
```xml

```
### 2. Setup properties

### 3. Configure IEConfigurer
```java
  @Bean
  public AutoContexConfigurer IEConfigurer(IEProperties properties) throws NoSuchFieldException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
      LOGGER.info("initiation IE Context...");
      AutoContexConfigurer autoContexConfigurer = new AutoContexConfigurer();
      autoContexConfigurer.init(properties);
      LOGGER.info("initiation IE Context success");
      return autoContexConfigurer;
  }
```
## 🚀 Future Plans

- CSV Processor
- JSON Processor
- UI for dynamically configuring import/export rules

# IE-Module

A powerful and extensible module for automated **Import** and **Export** of data.  
Built on top of **Apache POI**, it supports multiple database types and output formats such as **Excel**, **CSV**, and more.

---

## ✨ Features

- ✅ **Excel Processor**
  - Export data to Excel (`.xlsx`)
  - Import data from Excel files
  - Automatic mapping of database entities to Excel rows and columns
  - Support for custom column ordering and formatting
  - Multi-sheet support (define which fields appear in which sheet)
  - Ability to add links in cells
  - Automatic header generation

- ✅ **Database-Agnostic Design**
  - Works with any database supported by JPA/Hibernate
  - Extendable to support NoSQL sources
  - Handles lazy fetches and updates sessions automatically

- ✅ **Extensible Architecture**
  - Simple `Processor` interface for implementing custom input/output formats
  - **Flexible Data Mapping**: map entity fields to DTOs or transform field values (e.g., convert dates to strings, select specific fields)
  - **Error-tolerant Processing**: gracefully skips invalid records and continues processing

- ✅ **Error Handling & Logging**
  - Captures validation errors during import
  - Provides detailed logs for failed rows

- ✅ **Batch Processing**
  - Efficiently handles large datasets with batching support

---

## 🛠 How to Use

### 1. Add Maven Dependency

```xml
<dependency>
    <groupId>com.yourcompany</groupId>
    <artifactId>ie-module</artifactId>
    <version>1.0.0</version>
</dependency>
```

Or install manually:

```bash
mvn install:install-file -Dfile=ie-module.jar -DgroupId=com.yourcompany -DartifactId=ie-module -Dversion=1.0.0 -Dpackaging=jar
```

---

### 2. Configure Properties

IE-Module has several configurable properties. These can be set in `application.properties` or programmatically.

| Property               | Description                                           |
|-----------------------|-----------------------------------------------------|
| `ie.ie-report-path`   | Path where import/export progress reports are saved |
| `ie.ie-export-path`   | Path where exported data will be stored             |
| `ie.ie-template-path` | Path where templates are stored                     |
| `ie.package-to-scan`  | Package to scan for reportable classes              |

**Example (`application.properties`):**

```properties
ie.ie-report-path=/ie/importReport
ie.ie-export-path=/ie/export
ie.ie-template-path=/ie/template
ie.package-to-scan=com.example.package
```

---

### 3. Configure `IEConfigurer`

Register the context configuration bean in your Spring Boot project:

```java
@Bean
public AutoContexConfigurer ieConfigurer(IEProperties properties)
        throws NoSuchFieldException, InvocationTargetException, NoSuchMethodException,
               InstantiationException, IllegalAccessException {
    LOGGER.info("Initializing IE Context...");
    AutoContexConfigurer autoContexConfigurer = new AutoContexConfigurer();
    autoContexConfigurer.init(properties);
    LOGGER.info("IE Context initialized successfully.");
    return autoContexConfigurer;
}
```

---

### 4. Define Reportable Classes

For a DTO or Entity to be reportable, it must implement the appropriate interfaces:  
- `Exportable` for export  
- `Importable` for import  

#### Example: Exportable Class

Use the `@Reportable` annotation on each field you want included in the exported file.  
The `altName` attribute defines the column name; if not defined, the field name is used.  
The `order` attribute sets the column order.

```java
public class CustomerData implements Exportable<CustomerData> {

    @Reportable(order = 1, altName = "First Name")
    private String firstName;

    @Reportable(order = 2, altName = "Last Name")
    private String lastName;

    @Reportable
    private int age;

    // Getters and Setters
}
```

This makes IE-Module recognize this class as reportable.  
For Excel exports, implement `ExcelSheetConfigurer` to configure sheets:

```java
public class CustomerData implements Exportable<CustomerData>, ExcelSheetConfigurer<CustomerData> {

    @Reportable(order = 1, altName = "First Name")
    private String firstName;

    @Reportable(order = 2, altName = "Last Name")
    private String lastName;

    @Reportable
    private int age;

    @Reportable
    private List<String> adresses;

    // Getters and Setters

    @Override
    public Iterable<SheetConfig<CustomerData>> sheetConfigs() {
        List<SheetConfig<CustomerData>> sheetConfigs = new ArrayList<>();
        sheetConfigs.add(new SheetConfig<>(Stream.of("adresses").collect(Collectors.toSet()), "Customer Data"));
        sheetConfigs.add(new SheetConfig<>(Stream.of("age").collect(Collectors.toSet()), "Addresses", e -> e.status != null));
        return sheetConfigs;
    }
}
```

---

## 🚀 Future Plans

- CSV Processor  
- JSON Processor  
- Web UI for dynamically configuring import/export rules

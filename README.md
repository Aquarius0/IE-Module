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
  - Handles lazy fetches and session updates automatically

- ✅ **Extensible Architecture**
  - Simple `Processor` interface for implementing custom input/output formats
  - **Flexible Data Mapping**: map entity fields to DTOs or transform field values (e.g., convert dates to strings, select specific fields)
  - **Error-Tolerant Processing**: gracefully skips invalid records and continues processing

- ✅ **Error Handling & Logging**
  - Captures validation errors during import
  - Provides detailed logs for failed rows

- ✅ **Batch Processing**
  - Efficiently handles large datasets with batching support

---

## 🛠 How to Use

### 1. Add Maven Dependency

Add GitHub repository:

```xml
 <repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>
```

Add dependency:

```xml
<dependency>
    <groupId>com.github.aquarius0</groupId>
    <artifactId>ie-module</artifactId>
    <version>1.0.0</version>
</dependency>
```

Or install manually:

```bash
mvn install:install-file -Dfile=ie-module.jar \
    -DgroupId=com.github.aquarius0 -DartifactId=ie-module \
    -Dversion=1.0.0 -Dpackaging=jar
```

---

---

### 2. Configure Properties

IE-Module has several configurable properties. These can be set in `application.properties` or programmatically.

| Property               | Description                                           |
|------------------------|-----------------------------------------------------|
| `ie.ie-report-path`    | Path where import/export progress reports are saved |
| `ie.ie-export-path`    | Path where exported data will be stored             |
| `ie.ie-template-path`  | Path where templates are stored                     |
| `ie.package-to-scan`   | Package to scan for reportable classes              |

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
    private List<String> addresses;

    // Getters and Setters

    @Override
    public Iterable<SheetConfig<CustomerData>> sheetConfigs() {
        List<SheetConfig<CustomerData>> sheetConfigs = new ArrayList<>();
        sheetConfigs.add(new SheetConfig<>(Stream.of("addresses").collect(Collectors.toSet()), "Customer Data"));
        sheetConfigs.add(new SheetConfig<>(Stream.of("age").collect(Collectors.toSet()), "Addresses", e -> e.addresses != null));
        return sheetConfigs;
    }
}
```

---

### 5. Generating Reports

IE-Module has three main classes:  
1. **Processor**: Handles Input/Output files (Excel, CSV, etc.)  
2. **Task Runner**: Handles database operations (read/write)  
3. **Report Util**: Coordinates Processor and Task Runner

#### Define a Task Runner

```java
public class CustomerDataTaskRunner extends ExportTaskRunner<CustomerData> {
    private final CustomerDataService service;

    public CustomerDataTaskRunner(CustomerDataService service) {
        this.service = service;
    }

    @Override
    public void run() throws Exception {
        List<CustomerData> content = service.getAll();
        doExport(content);
    }
}
```

#### Start Export Process

```java
ExportTaskRunner<CustomerData> taskRunner = new CustomerDataTaskRunner(myCustomerService);
ExportProcessor<CustomerData> exportProcessor = new DefaultExcelExportProcessor<>("reportId", CustomerData.class);
String excelFilePath = IEUtils.exportData(taskRunner, exportProcessor);
```

#### Example: Importable Class

```java
public class CustomerData implements Importable<CustomerData> {

    @Reportable(order = 1, altName = "First Name")
    private String firstName;

    @Reportable(order = 2, altName = "Last Name")
    private String lastName;

    @Reportable
    private int age;

    // Getters and Setters

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CustomerData that = (CustomerData) o;
        return Objects.equals(firstName, that.firstName) && Objects.equals(lastName, that.lastName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName);
    }    
}
```

When an Importable entity has a `List` field, implement `merge()` to combine multiple records:

```java
@Override
public void merge(CustomerData customerData) {
    if (this.address == null) {
        this.address = new ArrayList<>();
    }
    address.addAll(customerData.address);
}
```

> **Note:**  
> Implement `equals()` and `hashCode()` to prevent duplication during import and merging.

For Excel imports, implement `ExcelSheetConfigurer` to configure sheets:

```java
public class CustomerData implements Importable<CustomerData>, ExcelSheetConfigurer<CustomerData> {

    @Reportable(order = 1, altName = "First Name")
    private String firstName;

    @Reportable(order = 2, altName = "Last Name")
    private String lastName;

    @Reportable
    private int age;

    @Reportable
    private List<String> address;

    // Getters and Setters


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CustomerData that = (CustomerData) o;
        return Objects.equals(firstName, that.firstName) && Objects.equals(lastName, that.lastName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName);
    }


    @Override
    public void merge(CustomerData customerData) {
        if(this.address == null){
            this.address=new ArrayList<>();
        }
        address.addAll(customerData.address);
    }

    @Override
    public Iterable<SheetConfig<CustomerData>> sheetConfigs() {
        List<SheetConfig<CustomerData>> sheetConfigs = new ArrayList<>();
        sheetConfigs.add(new SheetConfig<>(Stream.of("address").collect(Collectors.toSet()), "Customer Data"));
        sheetConfigs.add(new SheetConfig<>(Stream.of("age").collect(Collectors.toSet()), "Address", e -> e.address != null));
        return sheetConfigs;
    }
}
```

---

## 🚀 Future Plans

- CSV Processor  
- JSON Processor  
- UI for dynamically configuring import/export rules

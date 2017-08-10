# SCArchive
Tool for archiving documents and pictures

* SCArchive scans given local folders for different file types (PDF and HMTL by now, more are coming) and extracts meta data from each file.
* PDF Files are OCR'd and extracted with the help from PDFBox (https://pdfbox.apache.org/) tesseract (https://github.com/tesseract-ocr/tesseract) and Graphicsmagick (http://www.graphicsmagick.org/).  
* The application uses Vaadin for providing a Web-UI where the user can search for and edit the gathered meta data.
* As all files and also the gathered meta data is stored as local files, it is possible to synchronize the files via e.g. rsync or Resilio Sync to other machines.

## Technology Stack

* Java 8
* Spring-Boot
* Vaadin
* PDFBox
* tesseract
* GraphicsMagick

## Getting Started

1. Install the prerequisites
    * Java 8 or greater (https://www.java.com/de/download/)
    * tesseract (https://github.com/tesseract-ocr/tesseract#installing-tesseract)
    * GraphicsMagick (http://www.graphicsmagick.org/download.html)
    * Much RAM and CPU capacity (for OCR)
1. Currently only from source is possible
    * Clone this repository git clone git@github.com:scyv/SCArchive.git
    * Run `mvnw package`
    * Navigate to ./target: `cd target`
    * Copy application.properties from `src/main/resources`: `cp src/main/resources/application.properties .`
    * Edit application.properties for your needs (see below)
    * Run `java -jar server-0.0.1-SNAPSHOT.jar
    

## Application.properties

<table>
<thead>
    <tr>
        <th>Property key</th>
        <th>Possible Values</th>
        <th>Description</th>
    </tr>
</thead>
<tbody>
    <tr>
        <td>scarchive.documentpaths</td>
        <td>e.g. /home/user/myFiles;/home/user/myOtherFiles</td>
        <td>; separated list of folders, the application shall scan</td>
    </tr>
    <tr>
        <td>scarchive.scheduler.pollingInterval</td>
        <td>Integer e.g. 10</td>
        <td>Time between two scans in Seconds</td>
    </tr>
    <tr>
        <td>scarchive.tesseract.bin</td>
        <td>e.g. /usr/bin/tesseract</td>
        <td>Absolute path to the tesseract binary</td>
    </tr>
    <tr>
        <td>scarchive.graphicsmagick.bin</td>
        <td>e.g. /usr/bin/gm</td>
        <td>Absolute path to the graphicsmagick binary</td>
    </tr>
    <tr>
        <td>scarchive.openlocal</td>
        <td>true or false</td>
        <td>When true, the files are opened locally, when false, the files are downloaded</td>
    </tr>
    <tr>
        <td>scarchive.enablescan</td>
        <td>true or false</td>
        <td>When true, scanning of files is enabled, when false, no scanning takes place. This is especially useful if you want to provide the web ui without letting the host do the scanning</td>
    </tr>
    <tr>
        <td>scarchive.maxfindings</td>
        <td>e.g. 100</td>
        <td>Maximum amount of findings that shall be shown when searching for meta data</td>
    </tr>
</tbody>
</table>

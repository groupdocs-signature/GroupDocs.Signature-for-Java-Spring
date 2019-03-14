# GroupDocs.Signature-for-Java-Spring Example
###### version 1.7.12

[![Build Status](https://travis-ci.org/groupdocs-signature/GroupDocs.Signature-for-Java-Spring.svg?branch=master)](https://travis-ci.org/groupdocs-signature/GroupDocs.Signature-for-Java-Spring)
[![Maintainability](https://api.codeclimate.com/v1/badges/001a35ea4151759f0d2a/maintainability)](https://codeclimate.com/github/groupdocs-signature/GroupDocs.Signature-for-Java-Spring/maintainability)

## System Requirements
- Java 8 (JDK 1.8)
- Maven 3


## Description
GroupDocs.Signature is a native, simple, fully configurable and optimized web application which allows you to manipulate documents without requiring any other commercial application through GroupDocs APIs.

**Note** Without a license application will run in trial mode, purchase [GroupDocs.Signature for Java license](https://purchase.groupdocs.com/order-online-step-1-of-8.aspx) or request [GroupDocs.Signature for Java temporary license](https://purchase.groupdocs.com/temporary-license).


## Demo Video
https://youtu.be/MakhcqlV7iQ


## Features
- Clean, modern and intuitive design
- Easily switchable colour theme (create your own colour theme in 5 minutes)
- Responsive design
- Mobile support (open application on any mobile device)
- Support over 50 documents and image formats
- Image mode
- Fully customizable navigation panel
- Sign password protected documents
- Download original documents
- Download signed documents
- Upload documents
- Upload signatures
- Sign document with such signature types: digital certificate, image, stamp, qrCode, barCode.
- Draw signature image
- Draw stamp signature
- Generate bar code signature
- Generate qr code signature
- Print document
- Smooth page navigation
- Smooth document scrolling
- Preload pages for faster document rendering
- Multi-language support for displaying errors
- Cross-browser support (Safari, Chrome, Opera, Firefox)
- Cross-platform support (Windows, Linux, MacOS)


## How to run

You can run this sample by one of following methods


#### Build from source

Download [source code](https://github.com/groupdocs-signature/GroupDocs.Signature-for-Java-Spring/archive/master.zip) from github or clone this repository.

```bash
git clone https://github.com/groupdocs-signature/GroupDocs.Signature-for-Java-Spring
cd GroupDocs.Signature-for-Java-Spring
mvn clean spring-boot:run
## Open http://localhost:8080/signature/ in your favorite browser.
```

#### Build war from source

Download [source code](https://github.com/groupdocs-signature/GroupDocs.Signature-for-Java-Spring/archive/master.zip) from github or clone this repository.

```bash
git clone https://github.com/groupdocs-signature/GroupDocs.Signature-for-Java-Spring
cd GroupDocs.Signature-for-Java-Spring
mvn package -P war
## Deploy this war on any server
```

#### Binary release (with all dependencies)

Download [latest release](https://github.com/groupdocs-signature/GroupDocs.Signature-for-Java-Spring/releases/latest) from [releases page](https://github.com/groupdocs-signature/GroupDocs.Signature-for-Java-Spring/releases). 

**Note**: This method is **recommended** for running this sample behind firewall.

```bash
curl -J -L -o release.tar.gz https://github.com/groupdocs-signature/GroupDocs.Signature-for-Java-Spring/releases/download/1.7.12/release.tar.gz
tar -xvzf release.tar.gz
cd release
java -jar signature-spring-1.7.12.jar configuration.yml
## Open http://localhost:8080/signature/ in your favorite browser.
```

#### Docker image
Use [docker](https://www.docker.com/) image.

```bash
mkdir DocumentSamples
mkdir Licenses
docker run -p 8080:8080 --env application.hostAddress=localhost -v `pwd`/DocumentSamples:/home/groupdocs/app/DocumentSamples -v `pwd`/Licenses:/home/groupdocs/app/Licenses groupdocs/signature-for-java-spring
## Open http://localhost:8080/signature/ in your favorite browser.
```

#### Configuration
For all methods above you can adjust settings in `configuration.yml`. By default in this sample will lookup for license file in `./Licenses` folder, so you can simply put your license file in that folder or specify relative/absolute path by setting `licensePath` value in `configuration.yml`. 


## Resources
- **Website:** [www.groupdocs.com](http://www.groupdocs.com)
- **Product Home:** [GroupDocs.Signature for Java](https://products.groupdocs.com/signature/java)
- **Product API References:** [GroupDocs.Signature for Java API](https://apireference.groupdocs.com)
- **Download:** [Download GroupDocs.Signature for Java](http://downloads.groupdocs.com/signature/java)
- **Documentation:** [GroupDocs.Signature for Java Documentation](https://docs.groupdocs.com/display/signaturejava/Home)
- **Free Support Forum:** [GroupDocs.Signature for Java Free Support Forum](https://forum.groupdocs.com/c/signature)
- **Paid Support Helpdesk:** [GroupDocs.Signature for Java Paid Support Helpdesk](https://helpdesk.groupdocs.com)
- **Blog:** [GroupDocs.Signature for Java Blog](https://blog.groupdocs.com/category/groupdocs-signature-product-family)

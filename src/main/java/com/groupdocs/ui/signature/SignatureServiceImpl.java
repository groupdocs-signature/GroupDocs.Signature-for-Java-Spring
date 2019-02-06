package com.groupdocs.ui.signature;

import com.google.common.collect.Lists;
import com.groupdocs.signature.config.SignatureConfig;
import com.groupdocs.signature.domain.DocumentDescription;
import com.groupdocs.signature.domain.enums.HorizontalAlignment;
import com.groupdocs.signature.domain.enums.VerticalAlignment;
import com.groupdocs.signature.handler.SignatureHandler;
import com.groupdocs.signature.licensing.License;
import com.groupdocs.signature.options.OutputType;
import com.groupdocs.signature.options.SignatureOptionsCollection;
import com.groupdocs.signature.options.loadoptions.LoadOptions;
import com.groupdocs.signature.options.saveoptions.SaveOptions;
import com.groupdocs.ui.config.GlobalConfiguration;
import com.groupdocs.ui.exception.TotalGroupDocsException;
import com.groupdocs.ui.model.request.LoadDocumentPageRequest;
import com.groupdocs.ui.model.request.LoadDocumentRequest;
import com.groupdocs.ui.model.response.FileDescriptionEntity;
import com.groupdocs.ui.model.response.LoadDocumentEntity;
import com.groupdocs.ui.model.response.PageDescriptionEntity;
import com.groupdocs.ui.util.directory.SignatureDirectory;
import com.groupdocs.ui.signature.model.request.*;
import com.groupdocs.ui.signature.model.web.SignatureDataEntity;
import com.groupdocs.ui.signature.model.web.SignatureFileDescriptionEntity;
import com.groupdocs.ui.signature.model.web.SignaturePageEntity;
import com.groupdocs.ui.signature.model.web.SignedDocumentEntity;
import com.groupdocs.ui.signature.model.xml.*;
import com.groupdocs.ui.signature.signer.*;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.xml.bind.JAXBException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static com.groupdocs.ui.util.directory.PathConstants.DATA_FOLDER;
import static com.groupdocs.ui.util.directory.PathConstants.OUTPUT_FOLDER;
import static com.groupdocs.ui.signature.SignatureType.*;
import static com.groupdocs.ui.util.directory.SignatureDirectory.*;
import static com.groupdocs.ui.util.Utils.*;

@Service
public class SignatureServiceImpl implements SignatureService {

    private static final Logger logger = LoggerFactory.getLogger(SignatureServiceImpl.class);

    public static final String PNG = "png";
    private static final List<String> supportedImageFormats = Arrays.asList("bmp", "jpeg", "jpg", "tiff", "tif", PNG);

    private SignatureHandler signatureHandler;

    @Autowired
    private SignatureLoader signatureLoader;

    @Autowired
    private GlobalConfiguration globalConfiguration;

    @Autowired
    private SignatureConfiguration signatureConfiguration;

    /**
     * Initializing fields after creating configuration objects
     */
    @PostConstruct
    public void init() {
        // set GroupDocs license
        try {
            License license = new License();
            license.setLicense(globalConfiguration.getApplication().getLicensePath());
        } catch (Throwable exc) {
            logger.error("Can not verify Signature license!");
        }

        // check if data directory is set, if not set new directory
        if (signatureConfiguration.getDataDirectory() == null || signatureConfiguration.getDataDirectory().isEmpty()) {
            signatureConfiguration.setDataDirectory(signatureConfiguration.getFilesDirectory() + DATA_FOLDER);
        }

        // create directories
        createDirectories();

        // create signature application configuration
        SignatureConfig config = new SignatureConfig();
        config.setStoragePath(signatureConfiguration.getFilesDirectory());
        config.setCertificatesPath(getFullDataPath(CERTIFICATE_DATA_DIRECTORY.getPath()));
        config.setImagesPath(getFullDataPath(IMAGE_DATA_DIRECTORY.getPath()));
        config.setOutputPath(getFullDataPath(OUTPUT_FOLDER));

        // initialize total instance for the Image mode
        signatureHandler = new SignatureHandler(config);
    }

    private void createDirectories() {
        new File(getFullDataPath(CERTIFICATE_DATA_DIRECTORY.getPath())).mkdirs();
        new File(getFullDataPath(IMAGE_DATA_DIRECTORY.getPath())).mkdirs();

        new File(getFullDataPath(STAMP_DATA_DIRECTORY.getXMLPath())).mkdirs();
        new File(getFullDataPath(STAMP_DATA_DIRECTORY.getPreviewPath())).mkdirs();

        new File(getFullDataPath(QRCODE_DATA_DIRECTORY.getXMLPath())).mkdirs();
        new File(getFullDataPath(QRCODE_DATA_DIRECTORY.getPreviewPath())).mkdirs();

        new File(getFullDataPath(BARCODE_DATA_DIRECTORY.getXMLPath())).mkdirs();
        new File(getFullDataPath(BARCODE_DATA_DIRECTORY.getPreviewPath())).mkdirs();

        new File(getFullDataPath(TEXT_DATA_DIRECTORY.getXMLPath())).mkdirs();
        new File(getFullDataPath(TEXT_DATA_DIRECTORY.getPreviewPath())).mkdirs();
    }

    private String getFullDataPath(String partPath) {
        return signatureConfiguration.getDataDirectory() + partPath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SignatureConfiguration getSignatureConfiguration() {
        return signatureConfiguration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SignatureFileDescriptionEntity> getFileList(SignatureFileTreeRequest fileTreeRequest) {
        // get file list from storage path
        try {
            String signatureType = fileTreeRequest.getSignatureType() == null ? "" : fileTreeRequest.getSignatureType();
            String signatureTypePath = SignatureDirectory.getPathFromSignatureType(signatureType);
            String rootDirectory = StringUtils.isEmpty(signatureTypePath) ?
                    signatureConfiguration.getFilesDirectory() :
                    getFullDataPath(signatureTypePath);
            // get all the files from a directory
            String relDirPath = fileTreeRequest.getPath();
            if (StringUtils.isEmpty(relDirPath)) {
                relDirPath = rootDirectory;
            } else {
                relDirPath = String.format("%s%s%s", rootDirectory, File.separator, relDirPath);
            }
            List<SignatureFileDescriptionEntity> fileList;
            switch (signatureType) {
                case DIGITAL:
                    fileList = signatureLoader.loadFiles(relDirPath, signatureConfiguration.getDataDirectory());
                    break;
                case IMAGE:
                case HAND:
                    fileList = signatureLoader.loadImageSignatures(relDirPath, signatureConfiguration.getDataDirectory());
                    break;
                case STAMP:
                case TEXT:
                case QR_CODE:
                case BAR_CODE:
                    fileList = signatureLoader.loadStampSignatures(relDirPath, signatureConfiguration.getDataDirectory(), signatureType);
                    break;
                default:
                    fileList = signatureLoader.loadFiles(relDirPath, signatureConfiguration.getDataDirectory());
                    break;
            }
            return fileList;
        } catch (Exception ex) {
            logger.error("Exception occurred while getting file list", ex);
            throw new TotalGroupDocsException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LoadDocumentEntity getDocumentDescription(LoadDocumentRequest loadDocumentRequest) {
        String documentGuid = loadDocumentRequest.getGuid();
        String password = loadDocumentRequest.getPassword();
        try {
            // get document info container
            DocumentDescription documentDescription = signatureHandler.getDocumentDescription(documentGuid,
                    password);
            List<PageDescriptionEntity> pagesDescription = new ArrayList<>();
            // get info about each document page
            boolean loadData = signatureConfiguration.getPreloadPageCount() == 0;
            for(int i = 1; i <= documentDescription.getPageCount(); i++) {
                PageDescriptionEntity description = getPageDescriptionEntity(documentGuid, password, i, loadData);
                pagesDescription.add(description);
            }
            LoadDocumentEntity loadDocumentEntity = new LoadDocumentEntity();
            loadDocumentEntity.setGuid(loadDocumentRequest.getGuid());
            loadDocumentEntity.setPages(pagesDescription);
            // return document description
            return loadDocumentEntity;
        } catch (Exception ex) {
            logger.error("Exception occurred while loading document description", ex);
            throw new TotalGroupDocsException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageDescriptionEntity loadDocumentPage(LoadDocumentPageRequest loadDocumentPageRequest) {
        try {
            String documentGuid = loadDocumentPageRequest.getGuid();
            int pageNumber = loadDocumentPageRequest.getPage();
            String password = loadDocumentPageRequest.getPassword();
            // get page data
            PageDescriptionEntity pageDescriptionEntity = getPageDescriptionEntity(documentGuid, password, pageNumber, true);
            // return loaded page object
            return pageDescriptionEntity;
        } catch (Exception ex) {
            logger.error("Exception occurred while loading document page", ex);
            throw new TotalGroupDocsException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileDescriptionEntity saveStamp(SaveStampRequest saveStampRequest) {
        String previewPath = getFullDataPath(STAMP_DATA_DIRECTORY.getPreviewPath());
        String xmlPath = getFullDataPath(STAMP_DATA_DIRECTORY.getXMLPath());
        try {
            // get/set parameters
            String encodedImage = saveStampRequest.getImage().replace("data:image/png;base64,", "");
            List<StampXmlEntity> stampData = saveStampRequest.getStampData();

            FileDescriptionEntity savedImage = new FileDescriptionEntity();
            File file = getFile(previewPath, null);
            byte[] decodedImg = Base64.getDecoder().decode(encodedImage.getBytes(StandardCharsets.UTF_8));
            Files.write(file.toPath(), decodedImg);
            savedImage.setGuid(file.toPath().toString());
            // stamp data to xml file saving
            StampXmlEntityList stampXmlEntityList = new StampXmlEntityList();
            stampXmlEntityList.setStampXmlEntityList(stampData);
            String xmlFileName = FilenameUtils.removeExtension(file.getName());
            String fileName = String.format("%s%s%s.xml", xmlPath, File.separator, xmlFileName);
            new XMLReaderWriter<StampXmlEntityList>().write(fileName, stampXmlEntityList);
            // return loaded page object
            return savedImage;
        } catch (Exception ex) {
            logger.error("Exception occurred while saving stamp", ex);
            throw new TotalGroupDocsException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OpticalXmlEntity saveOpticalCode(SaveOpticalCodeRequest saveOpticalCodeRequest) {
        OpticalXmlEntity signatureData = saveOpticalCodeRequest.getProperties();
        String signatureType = saveOpticalCodeRequest.getSignatureType();
        // initiate signature data wrapper with default values
        SignatureDataEntity signatureDataEntity = getSignatureDataEntity(200, 270);
        // initiate signer object
        String previewPath;
        String xmlPath;
        // initiate signature options collection
        SignatureOptionsCollection collection = new SignatureOptionsCollection();
        // check optical signature type
        if (QR_CODE.equals(signatureType)) {
            QrCodeSigner qrSigner = new QrCodeSigner(signatureData, signatureDataEntity);
            // get preview path
            previewPath = getFullDataPath(QRCODE_DATA_DIRECTORY.getPreviewPath());
            // get xml file path
            xmlPath = getFullDataPath(QRCODE_DATA_DIRECTORY.getXMLPath());
            // generate unique file names for preview image and xml file
            collection.add(qrSigner.signImage());
        } else {
            BarCodeSigner barCodeSigner = new BarCodeSigner(signatureData, signatureDataEntity);
            // get preview path
            previewPath = getFullDataPath(BARCODE_DATA_DIRECTORY.getPreviewPath());
            // get xml file path
            xmlPath = getFullDataPath(BARCODE_DATA_DIRECTORY.getXMLPath());
            // generate unique file names for preview image and xml file
            collection.add(barCodeSigner.signImage());
        }
        try {
            if (signatureData.getTemp()) {
                BufferedImage bufImage = getBufferedImage(signatureDataEntity);
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ImageIO.write(bufImage, PNG, os);
                InputStream is = new ByteArrayInputStream(os.toByteArray());
                signWithImageToStream(signatureData, collection, is);
            } else {
                File file = writeImageFile(signatureData.getImageGuid(), signatureDataEntity, previewPath);
                String fileName = FilenameUtils.removeExtension(file.getName());
                // Save data to xml file
                new XMLReaderWriter<OpticalXmlEntity>().write(String.format("%s%s%s.xml", xmlPath, File.separator, fileName), signatureData);
                signWithImageToFile(previewPath, signatureData, collection, file.toPath().toString());
            }
        } catch (Exception e) {
            logger.error("Exception occurred while saving optical code signature", e);
            throw new TotalGroupDocsException(e.getMessage(), e);
        }
        signatureData.setWidth(signatureDataEntity.getImageWidth());
        signatureData.setHeight(signatureDataEntity.getImageHeight());
        return signatureData;
    }

    private PageDescriptionEntity getPageDescriptionEntity(String documentGuid, String password, int i, boolean withImage) throws Exception {
        PageDescriptionEntity description = new PageDescriptionEntity();
        // get current page size
        Dimension pageSize = signatureHandler.getDocumentPageSize(documentGuid, i, password, (double)0, (double)0, null);
        // set current page info for result
        description.setHeight(pageSize.getHeight());
        description.setWidth(pageSize.getWidth());
        description.setNumber(i);
        if (withImage) {
            loadImage(documentGuid, password, i, description);
        }
        return description;
    }

    private void loadImage(String documentGuid, String password, int i, PageDescriptionEntity description) throws Exception {
        byte[] pageImage = signatureHandler.getPageImage(documentGuid, i, password, null, 100);
        description.setData(new String(Base64.getEncoder().encode(pageImage)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TextXmlEntity saveText(SaveTextRequest saveTextRequest) {
        String previewPath = getFullDataPath(TEXT_DATA_DIRECTORY.getPreviewPath());
        String xmlPath = getFullDataPath(TEXT_DATA_DIRECTORY.getXMLPath());
        TextXmlEntity signatureData = saveTextRequest.getProperties();
        // initiate signature data wrapper with default values
        SignatureDataEntity signatureDataEntity = getSignatureDataEntity(signatureData.getWidth(), signatureData.getHeight());
        File file = writeImageFile(signatureData.getImageGuid(), signatureDataEntity, previewPath);
        try {
            String fileName = FilenameUtils.removeExtension(file.getName());
            // Save data to xml file
            new XMLReaderWriter<TextXmlEntity>().write(String.format("%s%s%s.xml", xmlPath, File.separator, fileName), signatureData);
        } catch (JAXBException e) {
            logger.error("Exception occurred while saving text signature", e);
            throw new TotalGroupDocsException(e.getMessage(), e);
        }
        // initiate signer object
        TextSigner textSigner = new TextSigner(signatureData, signatureDataEntity);
        // initiate signature options collection
        SignatureOptionsCollection collection = new SignatureOptionsCollection();
        // generate unique file names for preview image and xml file
        collection.add(textSigner.signImage());
        signWithImageToFile(previewPath, signatureData, collection, file.toPath().toString());
        return signatureData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileDescriptionEntity saveImage(SaveImageRequest saveImageRequest) {
        try {
            String dataDirectoryPath = getFullDataPath(IMAGE_DATA_DIRECTORY.getPath());
            String defaultImageName = "drawn signature.png";
            String imagePath = String.format("%s%s%s", dataDirectoryPath, File.separator, defaultImageName);
            File file = new File(imagePath);
            if (file.exists()) {
                String imageName = getFreeFileName(dataDirectoryPath, defaultImageName).toPath().getFileName().toString();
                imagePath = String.format("%s%s%s", dataDirectoryPath, File.separator, imageName);
                file = new File(imagePath);
            }
            String encodedImage = saveImageRequest.getImage().replace("data:image/png;base64,", "");
            byte[] decodedImg = Base64.getDecoder().decode(encodedImage.getBytes(StandardCharsets.UTF_8));
            Files.write(file.toPath(), decodedImg);

            FileDescriptionEntity savedImage = new FileDescriptionEntity();
            savedImage.setGuid(imagePath);
            // return loaded page object
            return savedImage;
        } catch (Exception ex) {
            logger.error("Exception occurred while saving image signature", ex);
            throw new TotalGroupDocsException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SignatureFileDescriptionEntity uploadDocument(MultipartFile content, String url, Boolean rewrite, String signatureType) {
        // get signatures storage path
        String pathFromSignatureType = signatureType == null ? "" : SignatureDirectory.getPathFromSignatureType(signatureType);
        String documentStoragePath = StringUtils.isEmpty(pathFromSignatureType) ?
                signatureConfiguration.getFilesDirectory() :
                getFullDataPath(pathFromSignatureType);
        // save the file
        String filePath = uploadFile(documentStoragePath, content, url, rewrite);
        // create response data
        SignatureFileDescriptionEntity uploadedDocument = new SignatureFileDescriptionEntity();
        uploadedDocument.setGuid(filePath);
        if (IMAGE.equals(signatureType)) {
            // get page image
            try {
                File file = new File(uploadedDocument.getGuid());
                // encode ByteArray into String
                String encodedImage = getStringFromStream(new FileInputStream(file));
                uploadedDocument.setImage(encodedImage);
            } catch (IOException ex) {
                logger.error("Exception occurred read images in document", ex);
                throw new TotalGroupDocsException(ex.getMessage(), ex);
            }
        }
        return uploadedDocument;
    }

    @Override
    public void deleteSignatureFile(DeleteSignatureFileRequest deleteSignatureFileRequest) {
        signatureLoader.deleteSignatureFile(deleteSignatureFileRequest);
    }

    @Override
    public List<String> getFonts() {
        GraphicsEnvironment ge = GraphicsEnvironment
                .getLocalGraphicsEnvironment();

        Font[] allFonts = ge.getAllFonts();

        List<String> response = new ArrayList<>(allFonts.length);

        for (Font font : allFonts) {
            response.add(font.getFontName());
        }

        return response;
    }

    @Override
    public SignaturePageEntity loadSignatureImage(LoadSignatureImageRequest loadSignatureImageRequest) {
        return signatureLoader.loadImage(loadSignatureImageRequest);
    }

    /**
     * Sign document by digital signature
     *
     * @param digital
     * @param documentType
     * @param signsCollection
     */
    private void signDigital(String password, List<SignatureDataEntity> digital, String documentType, SignatureOptionsCollection signsCollection) {
        try {
            for (int i = 0; i < digital.size(); i++) {
                SignatureDataEntity signatureDataEntity = digital.get(i);
                // initiate digital signer
                DigitalSigner signer = new DigitalSigner(signatureDataEntity, password);
                switch (documentType) {
                    case "Portable Document Format":
                        signsCollection.add(signer.signPdf());
                        break;
                    case "Microsoft Word":
                        signsCollection.add(signer.signWord());
                        break;
                    case "Microsoft Excel":
                        signsCollection.add(signer.signCells());
                        break;
                    default:
                        throw new IllegalStateException(String.format("File format %s is not supported.", documentType));
                }
            }
        } catch (Exception ex) {
            logger.error("Exception occurred while signing by digital signature", ex);
            throw new TotalGroupDocsException(ex.getMessage(), ex);
        }
    }

    /**
     * Sign document with images
     *
     * @param documentType
     * @param images
     * @param signsCollection
     * @return
     */
    private void signImage(String documentType, List<SignatureDataEntity> images, SignatureOptionsCollection signsCollection) {
        try {
            for (int i = 0; i < images.size(); i++) {
                SignatureDataEntity signatureDataEntity = images.get(i);
                // initiate image signer object
                ImageSigner signer = new ImageSigner(signatureDataEntity);
                // prepare signing options and sign document
                addSignOptions(documentType, signsCollection, signer);
            }
        } catch (Exception ex) {
            logger.error("Exception occurred while signing by image signature", ex);
            throw new TotalGroupDocsException(ex.getMessage(), ex);
        }
    }

    /**
     * Sign document with stamps
     *
     * @param documentType
     * @param stamps
     * @param signsCollection
     * @return
     */
    private void signStamp(String documentType, List<SignatureDataEntity> stamps, SignatureOptionsCollection signsCollection) {
        String xmlPath = getFullDataPath(STAMP_DATA_DIRECTORY.getXMLPath());
        try {
            for (int i = 0; i < stamps.size(); i++) {
                SignatureDataEntity signatureDataEntity = stamps.get(i);
                String fileName = getXMLFileName(xmlPath, signatureDataEntity.getSignatureGuid());
                StampXmlEntityList stampData = new XMLReaderWriter<StampXmlEntityList>().read(fileName, StampXmlEntityList.class);
                // since stamp ine are added stating from the most outer line we need to reverse the stamp data array
                List<StampXmlEntity> reverse = Lists.reverse(stampData.getStampXmlEntityList());
                // initiate stamp signer
                StampSigner signer = new StampSigner(reverse, signatureDataEntity);
                // prepare signing options and sign document
                addSignOptions(documentType, signsCollection, signer);
            }
        } catch (Exception ex) {
            logger.error("Exception occurred while signing by stamp", ex);
            throw new TotalGroupDocsException(ex.getMessage(), ex);
        }
    }

    private String getXMLFileName(String xmlPath, String signatureGuid) {
        // get xml data of the signature
        String xmlFileName = FilenameUtils.removeExtension(new File(signatureGuid).getName());
        // Load xml data
        return String.format("%s%s%s.xml", xmlPath, File.separator, xmlFileName);
    }

    /**
     * Sign document with barcodes and/or qrcodes
     *
     * @param documentType
     * @param codes
     * @param signsCollection
     * @return
     */
    private void signOptical(String documentType, List<SignatureDataEntity> codes, SignatureOptionsCollection signsCollection) {
        try {
            // prepare signing options and sign document
            for (int i = 0; i < codes.size(); i++) {
                SignatureDataEntity signatureDataEntity = codes.get(i);
                // get xml files root path
                String signatureType = signatureDataEntity.getSignatureType();
                String xmlPath = getFullDataPath((QR_CODE.equals(signatureType)) ?
                        QRCODE_DATA_DIRECTORY.getXMLPath() :
                        BARCODE_DATA_DIRECTORY.getXMLPath());
                // get xml data of the QR-Code
                String fileName = getXMLFileName(xmlPath, signatureDataEntity.getSignatureGuid());
                OpticalXmlEntity opticalCodeData = new XMLReaderWriter<OpticalXmlEntity>().read(fileName, OpticalXmlEntity.class);
                // initiate QRCode signer object
                Signer signer = (QR_CODE.equals(signatureType)) ? new QrCodeSigner(opticalCodeData, signatureDataEntity) : new BarCodeSigner(opticalCodeData, signatureDataEntity);
                // prepare signing options and sign document
                addSignOptions(documentType, signsCollection, signer);
            }
        } catch (Exception ex) {
            logger.error("Exception occurred while signing by optical code", ex);
            throw new TotalGroupDocsException(ex.getMessage(), ex);
        }
    }

    /**
     * Sign document with text signature
     *
     * @param documentType
     * @param texts
     * @param signsCollection
     * @return
     */
    private void signText(String documentType, List<SignatureDataEntity> texts, SignatureOptionsCollection signsCollection) {
        String xmlPath = getFullDataPath(TEXT_DATA_DIRECTORY.getXMLPath());
        try {
            // prepare signing options and sign document
            for (int i = 0; i < texts.size(); i++) {
                SignatureDataEntity signatureDataEntity = texts.get(i);
                // get xml data of the signature
                String fileName = getXMLFileName(xmlPath, signatureDataEntity.getSignatureGuid());
                TextXmlEntity textData = new XMLReaderWriter<TextXmlEntity>().read(fileName, TextXmlEntity.class);
                // initiate QRCode signer object
                TextSigner signer = new TextSigner(textData, signatureDataEntity);
                // prepare signing options and sign document
                addSignOptions(documentType, signsCollection, signer);
            }
        } catch (Exception ex) {
            logger.error("Exception occurred while signing by text signature", ex);
            throw new TotalGroupDocsException(ex.getMessage(), ex);
        }
    }

    @Override
    public SignedDocumentEntity sign(SignDocumentRequest signDocumentRequest) {
        String documentGuid = signDocumentRequest.getGuid();
        String documentType = getDocumentType(signDocumentRequest.getDocumentType(), documentGuid, FilenameUtils.getExtension(documentGuid));
        List<SignatureDataEntity> signaturesData = signDocumentRequest.getSignaturesData();
        SortedSignaturesData sortedSignaturesData = new SortedSignaturesData(signaturesData).sort(true);
        SignatureOptionsCollection signsCollection = new SignatureOptionsCollection();
        if (!sortedSignaturesData.digital.isEmpty()) {
            signDigital(signDocumentRequest.getPassword(), sortedSignaturesData.digital, documentType, signsCollection);
        }
        if (!sortedSignaturesData.images.isEmpty()) {
            signImage(documentType, sortedSignaturesData.images, signsCollection);
        }
        if (!sortedSignaturesData.texts.isEmpty()) {
            signText(documentType, sortedSignaturesData.texts, signsCollection);
        }
        if (!sortedSignaturesData.stamps.isEmpty()) {
            signStamp(documentType, sortedSignaturesData.stamps, signsCollection);
        }
        if (!sortedSignaturesData.codes.isEmpty()) {
            signOptical(documentType, sortedSignaturesData.codes, signsCollection);
        }
        return signDocument(documentGuid, signDocumentRequest.getPassword(), signsCollection);
    }

    /**
     * Write image to file
     *
     * @param imageGuid      image file guid if it exists
     * @param signaturesData signature
     * @param previewPath    path to file
     * @return
     */
    private File writeImageFile(String imageGuid, SignatureDataEntity signaturesData, String previewPath) {
        File file = getFile(previewPath, imageGuid);
        try {
            BufferedImage bufImage = getBufferedImage(signaturesData);
            // save BufferedImage to file
            ImageIO.write(bufImage, PNG, file);
        } catch (Exception ex) {
            logger.error("Exception occurred while saving signatures image", ex);
            throw new TotalGroupDocsException(ex.getMessage(), ex);
        }
        return file;
    }

    /**
     * Generate empty image for future signing with signature, such approach required to get signature as image
     *
     * @param signaturesData
     * @return
     */
    private BufferedImage getBufferedImage(SignatureDataEntity signaturesData) {
        BufferedImage bufImage = null;
        try {
            bufImage = new BufferedImage(signaturesData.getImageWidth(), signaturesData.getImageHeight(), BufferedImage.TYPE_INT_ARGB);
            // Create a graphics contents on the buffered image
            Graphics2D g2d = bufImage.createGraphics();
            // Draw graphics
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, signaturesData.getImageWidth(), signaturesData.getImageHeight());
            // Graphics context no longer needed so dispose it
            g2d.dispose();
            return bufImage;
        } catch (Exception ex) {
            logger.error("Exception occurred while saving signatures image", ex);
            throw new TotalGroupDocsException(ex.getMessage(), ex);
        } finally {
            if (bufImage != null) {
                bufImage.flush();
            }
        }
    }

    /**
     * Sign image with signature data for saving in stream
     *
     * @param signatureData signature
     * @param collection    signature options
     * @param inputStream   stream with image, for temporally sign
     */
    private void signWithImageToStream(XmlEntityWithImage signatureData, SignatureOptionsCollection collection, InputStream inputStream) {
        try {
            final SaveOptions saveOptions = new SaveOptions();
            saveOptions.setOutputType(OutputType.Stream);
            // sign generated image with signature
            SignatureConfig config = new SignatureConfig();
            config.setOutputPath(FileSystems.getDefault().getPath("").toAbsolutePath().toString());
            SignatureHandler<OutputStream> imgSignatureHandler = new SignatureHandler<>(config);
            ByteArrayOutputStream bos = (ByteArrayOutputStream) imgSignatureHandler.sign(inputStream, collection, saveOptions);
            byte[] bytes = bos.toByteArray();
            // encode ByteArray into String
            String encodedImage = Base64.getEncoder().encodeToString(bytes);
            signatureData.setEncodedImage(encodedImage);
        } catch (Exception ex) {
            logger.error("Exception occurred while saving optical code signature", ex);
            throw new TotalGroupDocsException(ex.getMessage(), ex);
        }
    }

    /**
     * Sign image with signature data for saving in local storage
     *
     * @param previewPath   local storage path
     * @param signatureData signature
     * @param collection    signature options
     * @param path          path to file
     */
    private void signWithImageToFile(String previewPath, XmlEntityWithImage signatureData, SignatureOptionsCollection collection, String path) {
        try {
            // set signing save options
            final SaveOptions saveOptions = new SaveOptions();
            saveOptions.setOutputType(OutputType.String);
            saveOptions.setOutputFileName(FilenameUtils.getName(path));
            saveOptions.setOverwriteExistingFiles(true);
            // set temporary signed documents path to image previews folder
            signatureHandler.getSignatureConfig().setOutputPath(previewPath);
            // sign generated image with signature
            signatureHandler.sign(path, collection, saveOptions);
            // set signed documents path back to correct path
            signatureHandler.getSignatureConfig().setOutputPath(getFullDataPath(OUTPUT_FOLDER));
            // set data for response
            signatureData.setImageGuid(path);
            // get signature preview as Base64 String
            byte[] bytes = signatureHandler.getPageImage(path, 1, "", null, 100);
            // encode ByteArray into String
            String encodedImage = Base64.getEncoder().encodeToString(bytes);
            signatureData.setEncodedImage(encodedImage);
        } catch (Exception ex) {
            logger.error("Exception occurred while saving optical code signature", ex);
            throw new TotalGroupDocsException(ex.getMessage(), ex);
        }
    }

    /**
     * Get filled signature data
     *
     * @param height
     * @param width
     * @return
     */
    private SignatureDataEntity getSignatureDataEntity(int height, int width) {
        SignatureDataEntity signatureDataEntity = new SignatureDataEntity();
        signatureDataEntity.setHorizontalAlignment(HorizontalAlignment.Center);
        signatureDataEntity.setVerticalAlignment(VerticalAlignment.Center);
        signatureDataEntity.setImageHeight(height);
        signatureDataEntity.setImageWidth(width);
        signatureDataEntity.setLeft(0);
        signatureDataEntity.setTop(0);
        return signatureDataEntity;
    }

    /**
     * Get fixed document type
     *
     * @param documentType
     * @param documentGuid
     * @param fileExtension
     * @return
     */
    private String getDocumentType(String documentType, String documentGuid, String fileExtension) {
        // mimeType should now be something like "image/png" if the document is image
        boolean isImage = supportedImageFormats.contains(fileExtension);
        return isImage ? "image" : documentType;
    }

    /**
     * Create file in previewPath and name imageGuid
     * if the file is already exist, create new file with next number in name
     * examples, 001, 002, 003, etc
     *
     * @param previewPath path to file folder
     * @param imageGuid   file name
     * @return created file
     */
    private File getFile(String previewPath, String imageGuid) {
        File folder = new File(previewPath);
        File[] listOfFiles = folder.listFiles();
        if (!StringUtils.isEmpty(imageGuid)) {
            return new File(imageGuid);
        } else {
            for (int i = 0; i <= listOfFiles.length; i++) {
                int number = i + 1;
                // set file name, for example 001
                String fileName = String.format("%03d", number);
                File file = new File(String.format("%s%s%s.png", previewPath, File.separator, fileName));
                // check if file with such name already exists
                if (file.exists()) {
                    continue;
                } else {
                    return file;
                }
            }
            return new File(String.format("%s%s001.png", previewPath, File.separator));
        }
    }

    /**
     * Add current signature options to signs collection
     *
     * @param documentType
     * @param signsCollection
     * @param signer
     * @throws ParseException
     */
    private void addSignOptions(String documentType, SignatureOptionsCollection signsCollection, Signer signer) throws ParseException {
        switch (documentType) {
            case "Portable Document Format":
                signsCollection.add(signer.signPdf());
                break;
            case "Microsoft Word":
                signsCollection.add(signer.signWord());
                break;
            case "Microsoft PowerPoint":
                signsCollection.add(signer.signSlides());
                break;
            case "image":
                signsCollection.add(signer.signImage());
                break;
            case "Microsoft Excel":
                signsCollection.add(signer.signCells());
                break;
            default:
                throw new IllegalStateException(String.format("File format %s is not supported.", documentType));
        }
    }

    /**
     * Sign document
     *
     * @param documentGuid
     * @param password
     * @param signsCollection
     * @return signed document
     * @throws Exception
     */
    private SignedDocumentEntity signDocument(String documentGuid, String password, SignatureOptionsCollection signsCollection) {
        // set save options
        final SaveOptions saveOptions = new SaveOptions();
        saveOptions.setOutputType(OutputType.String);
        saveOptions.setOutputFileName(new File(documentGuid).getName());

        // set password
        LoadOptions loadOptions = new LoadOptions();
        if (password != null && !password.isEmpty()) {
            loadOptions.setPassword(password);
        }

        // sign document
        SignedDocumentEntity signedDocument = new SignedDocumentEntity();
        try {
            signedDocument.setGuid(signatureHandler.sign(documentGuid, signsCollection, loadOptions, saveOptions).toString());
        } catch (Exception ex) {
            logger.error("Exception occurred while signing document", ex);
            throw new TotalGroupDocsException(ex.getMessage(), ex);
        }
        return signedDocument;
    }
}

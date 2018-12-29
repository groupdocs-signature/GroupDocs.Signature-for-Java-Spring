package com.groupdocs.ui.signature;

import com.google.common.collect.Ordering;
import com.groupdocs.ui.exception.TotalGroupDocsException;
import com.groupdocs.ui.model.response.LoadedPageEntity;
import com.groupdocs.ui.signature.model.request.DeleteSignatureFileRequest;
import com.groupdocs.ui.signature.model.request.LoadSignatureImageRequest;
import com.groupdocs.ui.signature.model.web.SignatureFileDescriptionEntity;
import com.groupdocs.ui.signature.model.xml.OpticalXmlEntity;
import com.groupdocs.ui.signature.model.xml.TextXmlEntity;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static com.groupdocs.ui.signature.PathConstants.DATA_PREVIEW_FOLDER;
import static com.groupdocs.ui.signature.PathConstants.DATA_XML_FOLDER;
import static com.groupdocs.ui.util.Utils.*;

/**
 * SignatureLoader
 * Loads signature files from the storage
 *
 * @author Aspose Pty Ltd
 */
@Service
public class SignatureLoader {

    /**
     * Load image signatures
     *
     * @return List<SignatureFileDescriptionEntity>
     * @param currentPath
     * @param dataPath
     */
    public List<SignatureFileDescriptionEntity> loadImageSignatures(String currentPath, String dataPath) {
        File directory = new File(currentPath);
        List<SignatureFileDescriptionEntity> fileList = new ArrayList<>();
        List<File> filesList = Arrays.asList(directory.listFiles());
        try {
            // sort list of files and folders
            filesList = Ordering.from(FILE_DATE_COMPARATOR).compound(FILE_NAME_COMPARATOR).sortedCopy(filesList);
            Path path = new File(dataPath).toPath();
            for (File file : filesList) {
                // check if current file/folder is hidden
                if (file.isHidden() || file.toPath().equals(path) || file.isDirectory()) {
                    // ignore current file and skip to next one
                    continue;
                } else {
                    SignatureFileDescriptionEntity fileDescription = getSignatureFileDescriptionEntity(file, true);
                    // add object to array list
                    fileList.add(fileDescription);
                }
            }
            return fileList;
        } catch (Exception ex) {
            throw new TotalGroupDocsException(ex.getMessage(), ex);
        }
    }

    /**
     * Load digital signatures or documents for signing
     *
     * @return List<SignatureFileDescriptionEntity>
     */
    public List<SignatureFileDescriptionEntity> loadFiles(String currentPath, String dataPath) {
        File directory = new File(currentPath);
        List<SignatureFileDescriptionEntity> fileList = new ArrayList<>();
        List<File> filesList = Arrays.asList(directory.listFiles());
        try {
            // sort list of files and folders
            filesList = Ordering.from(FILE_TYPE_COMPARATOR).compound(FILE_NAME_COMPARATOR).sortedCopy(filesList);
            Path path = new File(dataPath).toPath();
            for (File file : filesList) {
                // check if current file/folder is hidden
                if (file.isHidden() || file.toPath().equals(path)) {
                    // ignore current file and skip to next one
                    continue;
                } else {
                    SignatureFileDescriptionEntity fileDescription = getSignatureFileDescriptionEntity(file, false);
                    // add object to array list
                    fileList.add(fileDescription);
                }
            }
            return fileList;
        } catch (Exception ex) {
            throw new TotalGroupDocsException(ex.getMessage(), ex);
        }
    }

    /**
     * Load stamp signatures
     *
     * @return List<SignatureFileDescriptionEntity>
     */
    public List<SignatureFileDescriptionEntity> loadStampSignatures(String currentPath, String dataPath, String signatureType) {
        String imagesPath = currentPath + DATA_PREVIEW_FOLDER;
        String xmlPath = currentPath + DATA_XML_FOLDER;
        File images = new File(imagesPath);
        List<SignatureFileDescriptionEntity> fileList = new ArrayList<>();
        try {
            if(images.listFiles() != null) {
                List<File> imageFiles = Arrays.asList(images.listFiles());
                File xmls = new File(xmlPath);
                List<File> xmlFiles = Arrays.asList(xmls.listFiles());
                List<File> filesList = new ArrayList<>();
                for (File image : imageFiles) {
                    for (File xmlFile : xmlFiles) {
                        if (FilenameUtils.removeExtension(xmlFile.getName()).equals(FilenameUtils.removeExtension(image.getName()))) {
                            filesList.add(image);
                        }
                    }
                }
                // sort list of files and folders
                filesList = Ordering.from(FILE_DATE_COMPARATOR).compound(FILE_NAME_COMPARATOR).sortedCopy(filesList);
                Path path = new File(dataPath).toPath();
                for (File file : filesList) {
                    // check if current file/folder is hidden
                    if (file.isHidden() || file.toPath().equals(path) || file.isDirectory()) {
                        // ignore current file and skip to next one
                        continue;
                    } else {
                        SignatureFileDescriptionEntity fileDescription = getSignatureFileDescriptionEntity(file, true);
                        String fileName = getXmlFilePath(file);
                        if ("qrCode".equals(signatureType) || "barCode".equals(signatureType)) {
                            OpticalXmlEntity opticalCodeData = new XMLReaderWriter<OpticalXmlEntity>().read(fileName, OpticalXmlEntity.class);
                            fileDescription.setText(opticalCodeData.getText());
                        }
                        // add object to array list
                        fileList.add(fileDescription);
                    }
                }
            }
            return fileList;
        } catch (Exception ex){
            throw new TotalGroupDocsException(ex.getMessage(), ex);
        }
    }

    public void deleteSignatureFile(DeleteSignatureFileRequest deleteSignatureFileRequest) {
        String signatureType = deleteSignatureFileRequest.getSignatureType();
        if ("image".equals(signatureType) ||
                "digital".equals(signatureType)) {
            new File(deleteSignatureFileRequest.getGuid()).delete();
        } else {
            File file = new File(deleteSignatureFileRequest.getGuid());
            file.delete();
            String xmlFilePath = getXmlFilePath(file);
            new File(xmlFilePath).delete();
        }
    }

    private String getXmlFilePath(File file) {
        return file.getAbsolutePath().replace(DATA_PREVIEW_FOLDER, DATA_XML_FOLDER).replace(FilenameUtils.getExtension(file.getName()), "xml");
    }

    /**
     * Create file description
     *
     * @param file file
     * @param withImage set image
     * @return signature file description
     * @throws IOException
     */
    private SignatureFileDescriptionEntity getSignatureFileDescriptionEntity(File file, boolean withImage) throws IOException {
        SignatureFileDescriptionEntity fileDescription = new SignatureFileDescriptionEntity();
        fileDescription.setGuid(file.getAbsolutePath());
        fileDescription.setName(file.getName());
        // set is directory true/false
        fileDescription.setDirectory(file.isDirectory());
        // set file size
        fileDescription.setSize(file.length());
        if (withImage) {
            // get image Base64 encoded String
            FileInputStream fileInputStreamReader = new FileInputStream(file);
            byte[] bytes = new byte[(int) file.length()];
            fileInputStreamReader.read(bytes);
            fileDescription.setImage(Base64.getEncoder().encodeToString(bytes));
        }
        return fileDescription;
    }

    public LoadedPageEntity loadImage(LoadSignatureImageRequest loadSignatureImageRequest) {
        try {
            LoadedPageEntity loadedPage = new LoadedPageEntity();
            // get page image
            File file = new File(loadSignatureImageRequest.getGuid());
            byte[] bytes = Files.readAllBytes(file.toPath());
            // encode ByteArray into String
            String encodedImage = new String(Base64.getEncoder().encode(bytes));
            loadedPage.setPageImage(encodedImage);
            if ("text".equals(loadSignatureImageRequest.getSignatureType())) {
                String fileName = getXmlFilePath(file);
                TextXmlEntity textXmlEntity = new XMLReaderWriter<TextXmlEntity>().read(fileName, TextXmlEntity.class);
                loadedPage.setProps(textXmlEntity);
            }
            // return loaded page object
            return loadedPage;
        }catch (Exception ex){
            throw new TotalGroupDocsException(ex.getMessage(), ex);
        }
    }
}

package com.groupdocs.ui.signature;

import com.google.common.collect.Ordering;
import com.groupdocs.ui.exception.TotalGroupDocsException;
import com.groupdocs.ui.signature.model.web.SignatureFileDescriptionEntity;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

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
            filesList = Ordering.from(FILE_TYPE_COMPARATOR).compound(FILE_NAME_COMPARATOR).sortedCopy(filesList);
            Path path = new File(dataPath).toPath();
            for (File file : filesList) {
                // check if current file/folder is hidden
                if (!file.isDirectory() && checkFile(path, file)) {
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
        List<File> filesList = Arrays.asList(directory.listFiles());
        try {
            return getResultFileList(dataPath, filesList ,false);
        } catch (Exception ex) {
            throw new TotalGroupDocsException(ex.getMessage(), ex);
        }
    }

    private boolean checkFile(Path path, File file) {
        return !file.isHidden() && !file.toPath().equals(path);
    }

    /**
     * Load stamp signatures
     *
     * @return List<SignatureFileDescriptionEntity>
     */
    public List<SignatureFileDescriptionEntity> loadStampSignatures(String currentPath, String dataPath) {
        String imagesPath = currentPath + DATA_PREVIEW_FOLDER;
        String xmlPath = currentPath + DATA_XML_FOLDER;
        File images = new File(imagesPath);
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
                return getResultFileList(dataPath, filesList, true);
            }
            return Collections.EMPTY_LIST;
        } catch (Exception ex){
            throw new TotalGroupDocsException(ex.getMessage(), ex);
        }
    }

    private List<SignatureFileDescriptionEntity> getResultFileList(String dataPath, List<File> filesList, boolean withImage) throws IOException {
        List<SignatureFileDescriptionEntity> fileList = new ArrayList<>();
        // sort list of files and folders
        filesList = Ordering.from(FILE_TYPE_COMPARATOR).compound(FILE_NAME_COMPARATOR).sortedCopy(filesList);
        Path path = new File(dataPath).toPath();
        for (File file : filesList) {
            // check if current file/folder is hidden
            if (checkFile(path, file)) {
                SignatureFileDescriptionEntity fileDescription = getSignatureFileDescriptionEntity(file, withImage);
                // add object to array list
                fileList.add(fileDescription);
            }
        }
        return fileList;
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
            fileDescription.setImage(getStringFromStream(fileInputStreamReader));
        }
        return fileDescription;
    }

}

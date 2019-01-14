package com.groupdocs.ui.signature;

import com.groupdocs.ui.model.request.LoadDocumentPageRequest;
import com.groupdocs.ui.model.request.LoadDocumentRequest;
import com.groupdocs.ui.model.response.FileDescriptionEntity;
import com.groupdocs.ui.model.response.LoadDocumentEntity;
import com.groupdocs.ui.model.response.LoadedPageEntity;
import com.groupdocs.ui.signature.model.request.*;
import com.groupdocs.ui.signature.model.web.SignatureFileDescriptionEntity;
import com.groupdocs.ui.signature.model.web.SignedDocumentEntity;
import com.groupdocs.ui.signature.model.xml.OpticalXmlEntity;
import com.groupdocs.ui.signature.model.xml.TextXmlEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service for working with signature api
 */
public interface SignatureService {

    /**
     * Get configuration for Signature module
     *
     * @return SignatureConfiguration
     */
    SignatureConfiguration getSignatureConfiguration();

    /**
     * Get list of files in directory
     *
     * @param fileTreeRequest model with path parameter
     * @return list of files
     */
    List<SignatureFileDescriptionEntity> getFileList(SignatureFileTreeRequest fileTreeRequest);

    /**
     * Load document descriptions
     *
     * @param loadDocumentRequest document request data
     * @return list of document descriptions
     */
    LoadDocumentEntity getDocumentDescription(LoadDocumentRequest loadDocumentRequest);

    /**
     * Load document page
     *
     * @param loadDocumentPageRequest document page request data
     * @return loaded document page
     */
    LoadedPageEntity loadDocumentPage(LoadDocumentPageRequest loadDocumentPageRequest);

    /**
     * Save stamp signature
     *
     * @param saveStampRequest save signature request data
     * @return signature file description
     */
    FileDescriptionEntity saveStamp(SaveStampRequest saveStampRequest);

    /**
     * Save optical code signature
     *
     * @param saveOpticalCodeRequest save signature request data
     * @return optical code signature
     */
    OpticalXmlEntity saveOpticalCode(SaveOpticalCodeRequest saveOpticalCodeRequest);

    /**
     * Save test signature
     *
     * @param saveTextRequest save signature request data
     * @return text signature
     */
    TextXmlEntity saveText(SaveTextRequest saveTextRequest);

    /**
     * @param saveImageRequest save signature request data
     * @return signature file description
     */
    FileDescriptionEntity saveImage(SaveImageRequest saveImageRequest);

    /**
     * Upload document
     *
     * @param content       document content
     * @param url           document url
     * @param rewrite       flag for rewrite
     * @param signatureType type of signature
     * @return signature file description
     */
    SignatureFileDescriptionEntity uploadDocument(MultipartFile content, String url, Boolean rewrite, String signatureType);

    /**
     * Delete signature file from local storage
     *
     * @param deleteSignatureFileRequest
     */
    void deleteSignatureFile(DeleteSignatureFileRequest deleteSignatureFileRequest);

    /**
     * Get list of fonts names
     *
     * @return list of fonts names
     */
    List<String> getFonts();

    LoadedPageEntity loadSignatureImage(LoadSignatureImageRequest loadSignatureImageRequest);

    /**
     * Sign document
     *
     * @param signDocumentRequest
     * @return
     */
    SignedDocumentEntity sign(SignDocumentRequest signDocumentRequest);
}

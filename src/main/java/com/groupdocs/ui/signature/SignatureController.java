package com.groupdocs.ui.signature;

import com.groupdocs.ui.config.GlobalConfiguration;
import com.groupdocs.ui.exception.TotalGroupDocsException;
import com.groupdocs.ui.model.request.LoadDocumentPageRequest;
import com.groupdocs.ui.model.request.LoadDocumentRequest;
import com.groupdocs.ui.model.response.FileDescriptionEntity;
import com.groupdocs.ui.model.response.LoadDocumentEntity;
import com.groupdocs.ui.model.response.LoadedPageEntity;
import com.groupdocs.ui.signature.model.request.*;
import com.groupdocs.ui.signature.model.web.SignatureDataEntity;
import com.groupdocs.ui.signature.model.web.SignatureFileDescriptionEntity;
import com.groupdocs.ui.signature.model.web.SignedDocumentEntity;
import com.groupdocs.ui.signature.model.xml.OpticalXmlEntity;
import com.groupdocs.ui.signature.model.xml.TextXmlEntity;
import com.groupdocs.ui.util.Utils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nullable;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.groupdocs.ui.signature.PathConstants.OUTPUT_FOLDER;
import static com.groupdocs.ui.signature.SignatureType.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@Controller
@RequestMapping("/signature")
public class SignatureController {

    private static final Logger logger = LoggerFactory.getLogger(SignatureController.class);

    @Autowired
    private GlobalConfiguration globalConfiguration;

    @Autowired
    private SignatureService signatureService;

    /**
     * Get signature page
     * @param model model data for template
     * @return template name
     */
    @RequestMapping(method = RequestMethod.GET)
    public String getView(Map<String, Object> model){
        model.put("globalConfiguration", globalConfiguration);
        logger.debug("signature config: {}", signatureService.getSignatureConfiguration());
        model.put("signatureConfiguration", signatureService.getSignatureConfiguration());
        return "signature";
    }

    /**
     * Get files and directories
     * @return files and directories list
     */
    @RequestMapping(method = RequestMethod.POST, value = "/loadFileTree", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<SignatureFileDescriptionEntity> loadFileTree(@RequestBody SignatureFileTreeRequest fileTreeRequest){
        return signatureService.getFileList(fileTreeRequest);
    }

    /**
     * Get document description
     * @return document description
     */
    @RequestMapping(method = RequestMethod.POST, value = "/loadDocumentDescription", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public LoadDocumentEntity loadDocumentDescription(@RequestBody LoadDocumentRequest loadDocumentRequest) {
        return signatureService.getDocumentDescription(loadDocumentRequest);
    }

    /**
     * Get document page
     * @return document page
     */
    @RequestMapping(method = RequestMethod.POST, value = "/loadDocumentPage", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public LoadedPageEntity loadDocumentPage(@RequestBody LoadDocumentPageRequest loadDocumentPageRequest) {
        return signatureService.loadDocumentPage(loadDocumentPageRequest);
    }

    /**
     * Get fonts
     * @return list of fonts names
     */
    @RequestMapping(method = RequestMethod.GET, value = "/getFonts", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<String> getFonts() {
        return signatureService.getFonts();
    }

    /**
     * Download document
     * @param response
     * @return document
     */
    @RequestMapping(method = RequestMethod.GET, value = "/downloadDocument")
    public void downloadDocument(@RequestParam(name = "path") String documentGuid,
                                 @RequestParam(name = "signed") Boolean signed,
                                 HttpServletResponse response) {
        // get document path
        String fileName = FilenameUtils.getName(documentGuid);
        // choose directory
        SignatureConfiguration signatureConfiguration = signatureService.getSignatureConfiguration();
        String filesDirectory = signed ? signatureConfiguration.getDataDirectory() + OUTPUT_FOLDER : signatureConfiguration.getFilesDirectory();
        String pathToDownload = String.format("%s%s%s", filesDirectory, File.separator, fileName);

        // set response content info
        Utils.addFileDownloadHeaders(response, fileName, null);

        long length;
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(pathToDownload));
             ServletOutputStream outputStream = response.getOutputStream()) {
            // download the document
            length = IOUtils.copyLarge(inputStream, outputStream);
        } catch (Exception ex){
            logger.error("Exception in downloading document", ex);
            throw new TotalGroupDocsException(ex.getMessage(), ex);
        }

        Utils.addFileDownloadLengthHeader(response, length);
    }

    /**
     * Upload document
     * @return uploaded document object (the object contains uploaded document guid)
     */
    @RequestMapping(method = RequestMethod.POST, value = "/uploadDocument",
            consumes = MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public SignatureFileDescriptionEntity uploadDocument(@Nullable @RequestParam("file") MultipartFile content,
                                                         @RequestParam(value = "url", required = false) String url,
                                                         @RequestParam("rewrite") Boolean rewrite,
                                                         @RequestParam(value = "signatureType", required = false) String signatureType) {
        return signatureService.uploadDocument(content, url, rewrite, signatureType);
    }

    /**
     * Get signature image stream - temporally workaround used until release of the GroupDocs.Signature 18.5, after release will be removed
     * @return signature image
     */
    @RequestMapping(method = RequestMethod.POST, value = "/loadSignatureImage", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public LoadedPageEntity loadSignatureImage(@RequestBody LoadSignatureImageRequest loadSignatureImageRequest) {
        return signatureService.loadSignatureImage(loadSignatureImageRequest);
    }

    /**
     * Delete signature file from local storage
     */
    @RequestMapping(method = RequestMethod.POST, value = "/deleteSignatureFile", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public void deleteSignatureFile(@RequestBody DeleteSignatureFileRequest deleteSignatureFileRequest){
        signatureService.deleteSignatureFile(deleteSignatureFileRequest);
    }

    /**
     * Sign document with signatures
     * @return signed document info
     */
    @RequestMapping(method = RequestMethod.POST, value = "/sign", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public SignedDocumentEntity signDigital(@RequestBody SignDocumentRequest signDocumentRequest){
        List<SignatureDataEntity> signaturesData = signDocumentRequest.getSignaturesData();
        if (signaturesData == null || signaturesData.isEmpty()) {
            throw new IllegalArgumentException("Sign data is empty");
        }
        String documentGuid = signDocumentRequest.getGuid();
        String password = signDocumentRequest.getPassword();
        String documentType = signDocumentRequest.getDocumentType();
        SignedDocumentEntity signedDocumentEntity = new SignedDocumentEntity();
        List<SignatureDataEntity> images = new ArrayList<>();
        List<SignatureDataEntity> texts = new ArrayList<>();
        List<SignatureDataEntity> codes = new ArrayList<>();
        List<SignatureDataEntity> stamps = new ArrayList<>();
        for (int i = 0; i < signaturesData.size(); i++) {
            SignatureDataEntity signatureDataEntity = signaturesData.get(i);
            switch (signatureDataEntity.getSignatureType()) {
                case TEXT:
                    texts.add(signatureDataEntity);
                    break;
                case DIGITAL:
                    signedDocumentEntity = signatureService.signDigital(documentGuid, password, signatureDataEntity, documentType);
                    break;
                case IMAGE:
                case HAND:
                    images.add(signatureDataEntity);
                    break;
                case STAMP:
                    stamps.add(signatureDataEntity);
                    break;
                case QR_CODE:
                case BAR_CODE:
                    codes.add(signatureDataEntity);
                    break;
                default:
                    throw new IllegalArgumentException("Signature type is wrong");
            }
        }
        if (!images.isEmpty()) {
            signedDocumentEntity = signatureService.signImage(documentGuid, password, documentType, images);
        }
        if (!texts.isEmpty()) {
            signedDocumentEntity = signatureService.signText(documentGuid, password, documentType, texts);
        }
        if (!stamps.isEmpty()) {
            signedDocumentEntity = signatureService.signStamp(documentGuid, password, documentType, stamps);
        }
        if (!codes.isEmpty()) {
            signedDocumentEntity = signatureService.signOptical(documentGuid, password, documentType, codes);
        }
        return signedDocumentEntity;
    }

    /**
     * Save signature image stream
     * @return image signature
     */
    @RequestMapping(method = RequestMethod.POST, value = "/saveImage", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public FileDescriptionEntity saveImage(@RequestBody SaveImageRequest saveImageRequest){
        return signatureService.saveImage(saveImageRequest);
    }

    /**
     * Save signature stamp
     * @return stamp
     */
    @RequestMapping(method = RequestMethod.POST, value = "/saveStamp", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public FileDescriptionEntity saveStamp(@RequestBody SaveStampRequest saveStampRequest) {
        return signatureService.saveStamp(saveStampRequest);
    }

    /**
     * Save Optical signature data
     * @return optical signature
     */
    @RequestMapping(method = RequestMethod.POST, value = "/saveOpticalCode", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public OpticalXmlEntity saveOpticalCode(@RequestBody SaveOpticalCodeRequest saveOpticalCodeRequest){
        return signatureService.saveOpticalCode(saveOpticalCodeRequest);
    }

    /**
     * Save signature text
     * @return text signature
     */
    @RequestMapping(method = RequestMethod.POST, value = "/saveText", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public TextXmlEntity saveText(@RequestBody SaveTextRequest saveTextRequest){
        return signatureService.saveText(saveTextRequest);
    }
}

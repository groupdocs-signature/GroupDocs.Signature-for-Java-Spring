package com.groupdocs.ui.signature.signer;

import com.groupdocs.signature.domain.stamps.StampBackgroundCropType;
import com.groupdocs.signature.domain.stamps.StampLine;
import com.groupdocs.signature.domain.stamps.StampTextRepeatType;
import com.groupdocs.signature.options.stampsignature.*;
import com.groupdocs.ui.signature.model.web.SignatureDataEntity;
import com.groupdocs.ui.signature.model.xml.StampXmlEntity;

import java.util.List;

/**
 * StampSigner
 * Signs documents with the stamp signature
 *
 * @author Aspose Pty Ltd
 */
public class StampSigner extends Signer {
    private List<StampXmlEntity> stampData;

    /**
     * Constructor
     *
     * @param stampData
     * @param signatureData
     */
    public StampSigner(List<StampXmlEntity> stampData, SignatureDataEntity signatureData) {
        super(signatureData);
        this.stampData = stampData;
    }

    /**
     * Add stamp signature data to pdf sign options
     *
     * @return PdfStampSignOptions
     */
    @Override
    public PdfStampSignOptions signPdf() {
        // setup options
        PdfStampSignOptions signOptions = new PdfStampSignOptions();
        fillStampOptions(signOptions);
        return signOptions;
    }

    private void fillStampOptions(StampSignOptions signOptions) {
        signOptions.setHeight(signatureData.getImageHeight());
        signOptions.setWidth(signatureData.getImageWidth());
        signOptions.setTop(signatureData.getTop());
        signOptions.setLeft(signatureData.getLeft());
        signOptions.setDocumentPageNumber(signatureData.getPageNumber());
        signOptions.setRotationAngle(signatureData.getAngle());
        signOptions.setBackgroundColor(getColor(stampData.get(stampData.size() - 1).getBackgroundColor()));
        signOptions.setBackgroundColorCropType(StampBackgroundCropType.OuterArea);
        fillStamp(signOptions.getInnerLines(), signOptions.getOuterLines());
    }

    /**
     * Add stamp signature data to image sign options
     *
     * @return ImageStampSignOptions
     */
    @Override
    public ImagesStampSignOptions signImage() {
        // setup options
        ImagesStampSignOptions signOptions = new ImagesStampSignOptions();
        fillStampOptions(signOptions);
        return signOptions;
    }

    /**
     * Add stamp signature data to words sign options
     *
     * @return WordsStampSignOptions
     */
    @Override
    public WordsStampSignOptions signWord() {
        // setup options
        WordsStampSignOptions signOptions = new WordsStampSignOptions();
        fillStampOptions(signOptions);
        return signOptions;
    }

    /**
     * Add stamp signature data to cells sign options
     *
     * @return CellsStampSignOptions
     */
    @Override
    public CellsStampSignOptions signCells() {
        // setup options
        CellsStampSignOptions signOptions = new CellsStampSignOptions();
        fillStampOptions(signOptions);
        return signOptions;
    }

    /**
     * Add stamp signature data to slides sign options
     *
     * @return SlidesStampSignOptions
     */
    @Override
    public SlidesStampSignOptions signSlides() {
        // setup options
        SlidesStampSignOptions signOptions = new SlidesStampSignOptions();
        fillStampOptions(signOptions);
        return signOptions;
    }

    private void fillStamp(List<StampLine> innerLines, List<StampLine> outerLines) {
        for (int n = 0; n < stampData.size(); n++) {
            String text = "";
            for (int m = 0; m < stampData.get(n).getTextRepeat(); m++) {
                text = text + stampData.get(n).getText();
            }
            // set reduction size - required to recalculate each stamp line height and font size after stamp resizing in the UI
            int reductionSize = 0;
            // check if reduction size is between 1 and 2. for example: 1.25
            if ((double) stampData.get(n).getHeight() / signatureData.getImageHeight() > 1 && (double) stampData.get(n).getHeight() / signatureData.getImageHeight() < 2) {
                reductionSize = 2;
            } else if (stampData.get(n).getHeight() / signatureData.getImageHeight() == 0) {
                reductionSize = 1;
            } else {
                reductionSize = stampData.get(n).getHeight() / signatureData.getImageHeight();
            }
            if ((n + 1) == stampData.size()) {
                // draw inner horizontal line
                StampLine squareLine = new StampLine();
                squareLine.setText(text);
                squareLine.getFont().setFontSize(stampData.get(n).getFontSize() / reductionSize);
                squareLine.setTextColor(getColor(stampData.get(n).getTextColor()));
                innerLines.add(squareLine);
                if (stampData.size() == 1) {
                    StampLine line = initStampLine(n);
                    line.getInnerBorder().setColor(getColor(stampData.get(n).getBackgroundColor()));
                    line.setHeight(1);
                    outerLines.add(line);
                }
            } else {
                // draw outer rounded lines
                StampLine line = initStampLine(n);
                line.getInnerBorder().setColor(getColor(stampData.get(n + 1).getStrokeColor()));
                int height = (stampData.get(n).getRadius() - stampData.get(n + 1).getRadius()) / reductionSize;
                line.setHeight(height);
                line.setText(text);
                line.getFont().setFontSize(stampData.get(n).getFontSize() / reductionSize);
                line.setTextColor(getColor(stampData.get(n).getTextColor()));
                line.setTextBottomIntent((height / 2));
                line.setTextRepeatType(StampTextRepeatType.RepeatWithTruncation);
                outerLines.add(line);
            }
        }
    }

    private StampLine initStampLine(int n) {
        StampLine line = new StampLine();
        line.setBackgroundColor(getColor(stampData.get(n).getBackgroundColor()));
        line.getOuterBorder().setColor(getColor(stampData.get(n).getStrokeColor()));
        line.getOuterBorder().setWeight(0.5);
        line.getInnerBorder().setWeight(0.5);
        return line;
    }

}

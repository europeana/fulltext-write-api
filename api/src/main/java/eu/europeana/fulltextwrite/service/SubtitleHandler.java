package eu.europeana.fulltextwrite.service;

import static eu.europeana.fulltextwrite.AppConstants.defaultSubtitleConfig;

import com.dotsub.converter.exception.FileFormatException;
import com.dotsub.converter.importer.SubtitleImportHandler;
import com.dotsub.converter.model.SubtitleItem;
import eu.europeana.fulltextwrite.exception.FTWriteInstantiationException;
import eu.europeana.fulltextwrite.exception.InvalidFormatException;
import eu.europeana.fulltextwrite.model.SubtitleType;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class SubtitleHandler {

  /**
   * parses the text to Subtitle Item
   *
   * @param text
   * @param subtitleType
   * @return
   * @throws FTWriteInstantiationException
   * @throws FileFormatException
   * @throws IOException
   */
  public List<SubtitleItem> parseSubtitle(InputStream text, SubtitleType subtitleType)
      throws FTWriteInstantiationException, InvalidFormatException, IOException {
    SubtitleImportHandler subtitleImportHandler = getImportHandler(subtitleType);
    if (subtitleImportHandler == null) {
      throw new InvalidFormatException("Format not supported : " + subtitleType.getMimeType());
    }
    try {
      return subtitleImportHandler.importFile(text, defaultSubtitleConfig);
    } catch (FileFormatException e) {
      throw new InvalidFormatException(
          "Please provide proper format!! File does not match the expected format - "
              + subtitleType.getMimeType());
    }
  }

  /**
   * Instantiates the suitable Subtitle Import Handler to convert the data to Subtitles
   *
   * @param subtitleType
   * @return
   * @throws FTWriteInstantiationException
   */
  public SubtitleImportHandler getImportHandler(SubtitleType subtitleType)
      throws FTWriteInstantiationException {
    try {
      return (SubtitleImportHandler)
          Class.forName(subtitleType.getHandler()).getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      throw new FTWriteInstantiationException("Failed to instantiate handler for subtitle");
    }
  }
}

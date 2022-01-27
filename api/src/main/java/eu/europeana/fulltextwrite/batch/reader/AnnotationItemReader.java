package eu.europeana.fulltextwrite.batch.reader;

import eu.europeana.fulltextwrite.model.external.AnnotationItem;
import eu.europeana.fulltextwrite.service.AnnotationsApiRestService;
import java.util.Iterator;
import java.util.List;
import org.springframework.batch.item.data.AbstractPaginatedDataItemReader;

public class AnnotationItemReader extends AbstractPaginatedDataItemReader<AnnotationItem> {

  private final AnnotationsApiRestService annotationsRestService;

  public AnnotationItemReader(AnnotationsApiRestService annotationsRestService, int pageSize) {
    setPageSize(pageSize);
    this.annotationsRestService = annotationsRestService;
    // Non-restartable, as we expect this to run in multi-threaded steps.
    // see: https://stackoverflow.com/a/20002493
    setSaveState(false);
  }

  String getClassName() {
    return AnnotationItemReader.class.getSimpleName();
  }

  @Override
  protected Iterator<AnnotationItem> doPageRead() {
    // pageSize is incremented in parent class every time this method is invoked
    List<AnnotationItem> searchResponse = annotationsRestService.getAllItems(page, pageSize);
    return searchResponse.iterator();
  }

  @Override
  protected void doOpen() throws Exception {
    super.doOpen();
    setName(getClassName());
  }
}

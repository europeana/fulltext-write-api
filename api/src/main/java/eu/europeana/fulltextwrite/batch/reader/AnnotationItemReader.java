package eu.europeana.fulltextwrite.batch.reader;

import eu.europeana.fulltextwrite.model.external.AnnotationItem;
import eu.europeana.fulltextwrite.service.AnnotationsApiRestService;
import java.util.Iterator;
import java.util.List;
import org.springframework.batch.item.data.AbstractPaginatedDataItemReader;

public class AnnotationItemReader extends AbstractPaginatedDataItemReader<AnnotationItem> {

  private final AnnotationsApiRestService annotationsRestService;

  public AnnotationItemReader(AnnotationsApiRestService annotationsRestService, int pageSize) {
    this.annotationsRestService = annotationsRestService;
    setPageSize(pageSize);
    // Non-restartable, as we expect this to run in multi-threaded steps.
    // see: https://stackoverflow.com/a/20002493
    setSaveState(false);
  }

  String getClassName() {
    return AnnotationItemReader.class.getSimpleName();
  }

  @Override
  protected Iterator<AnnotationItem> doPageRead() {
    // number of items to skip when reading. pageSize is incremented in parent class every time
    // this method is invoked
    int start = page * pageSize;

    List<AnnotationItem> searchResponse = annotationsRestService.getAllItems(start, pageSize);
    return searchResponse.iterator();
  }

  @Override
  protected void doOpen() throws Exception {
    super.doOpen();
    setName(getClassName());
  }
}

package util.spring;

import javax.sql.DataSource;

public class DivisionDao {
  private final DataSource dataSource;

  public DivisionDao(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public DataSource getDataSource() {
    return dataSource;
  }
}
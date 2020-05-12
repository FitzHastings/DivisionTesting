package scripts.groovy

def data = session.executeQuery("SELECT [CreatedDocument(dealPositions):object],[CreatedDocument(dealPositions):target] FROM [CreatedDocument(dealPositions):table] "
  +"WHERE [CreatedDocument(dealPositions):target] IN (SELECT [DealPosition(id)] FROM [DealPosition] WHERE [DealPosition(deal)] IN "
  +"(SELECT [DealPosition(deal)] FROM [DealPosition] WHERE [DealPosition(id)]=ANY(?)))", [dealPositions].toArray());

def documents = [:]
data.each {
  if(documents[it[0]] == null)
    documents[it[0]] = []
  documents[it[0]] << it[1]
}


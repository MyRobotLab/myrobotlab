#########################################
# DatabaseConnector.py
# more info @: http://myrobotlab.org/service/DatabaseConnector
#########################################

database = runtime.start("database","DatabaseConnector")
database.setDriver("com.mysql.jdbc.Driver")
database.connectionString="jdbc:mysql://localhost/sakila"
database.jdbcUser="user"
database.jdbcPassword="password"

# crawlers publish documents
def onDocument(doc):
    print(doc)

database.addListener("publishDocument","python","onDocument")

database.setIdField("actor_id")
database.setSql("select actor_id, first_name, last_name from actor")

if not ('virtual' in globals() and virtual):
# start crawling
  database.startCrawling()
  sleep(5)
  database.stopCrawling()
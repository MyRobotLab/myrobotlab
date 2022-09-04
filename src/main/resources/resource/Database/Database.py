#########################################
# Database.py
# description: database - wrapper around jdbc access
# categories: storage
# more info @: http://myrobotlab.org/service/Database
#########################################

if ('virtual' in globals() and virtual):virtual=True
else:virtual=False

# start the service
database = runtime.start('database','Database')
database.connectionString="jdbc:mysql://HOST/DATABASE"
database.jdbcUser="user"
database.jdbcPassword="password"

if not virtual:
  print database.connect()
  resultSet  = database.executeQuery("SELECT * FROM YOUR_TABLE")
  while (resultSet.next()):
    print resultSet.getString("id")
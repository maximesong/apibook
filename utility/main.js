var program = require('commander');
var MongoClient = require('mongodb').MongoClient
var mongoUrl = 'mongodb://localhost:27017/apibook'

program
  .version('0.0.1')
  .option('-n, --number', 'number')

program
  .command('test')
  .description('this command is for test')
  .action(function(cmd, options) {
    console.log('running testing...');
    read();
  })

function read() {
  MongoClient.connect(mongoUrl, function(err, db) {
    var questions = db.collection("questions")
    questions.find({}).toArray(function(err, data) {
      console.log(JSON.stringify(data, null, 4));
      db.close();
    })
    console.log(err);
  })
}
program.parse(process.argv);

var program = require('commander');

program
  .version('0.0.1')
  .option('-n, --number', 'number')

program
  .command('test <cmd>')
  .description('this command is for test')
  .action(function(cmd, options) {
    console.log(options)
    console.log('testing!')
  })

program.parse(process.argv);

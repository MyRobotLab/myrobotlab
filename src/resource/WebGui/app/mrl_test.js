//Everything here is toying aroung!

// describe('mrlapp.mrl module', function() {

  // beforeEach(module('mrlapp.mrl'));

  // describe('mrl service', function(){

    // it('should ....', inject(function($service) {
      // //spec body
      // var mrlService = $service('mrl');
      // expect(mrlService).toBeDefined();
    // }));

  // });
// });
describe('A suite', function() {
  var testMe;

  beforeEach(function() {
    testMe = true;
  });

  it('should be true', function() {
    expect(testMe).toBe(true);
  });
});
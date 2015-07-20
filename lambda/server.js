console.log('Loading event');

var AWS = require('aws-sdk');
var db = new AWS.DynamoDB({params: {TableName: "digits-with-lambda"}});

var User = {

    get : function(phoneNumber, callback) {
        var query = {
            Key: {
                phoneNumber: { S: phoneNumber }
            }
        };
        db.getItem(query, callback);
    },

    insert : function(phoneNumber, id, accessToken, accessTokenSecret, name, status, callback) {
        var obj = User.makeObject(phoneNumber, id, accessToken, accessTokenSecret, name, status);
        var item = {
                Item: obj
            };
        db.putItem(item, callback);
    },

    update : function(phoneNumber, id, accessToken, accessTokenSecret, name, status, callback) {
        var obj = User.makeObject(phoneNumber, id, accessToken, accessTokenSecret, name, status);
        var updates = {
            Key: {
                phoneNumber: { S: phoneNumber }
            },
            AttributeUpdates: obj
        };
        
        db.updateItem(updates, callback);
    },
    
    makeObject : function(phoneNumber, id, accessToken, accessTokenSecret, name, status){

        var obj = {
            phoneNumber: phoneNumber,
            id: id,
            accessToken : accessToken,
            accessTokenSecret : accessTokenSecret,
            name : name,
            status : status
        };
        
        return Utils.dbWrap(obj);
    }

};

var Status = {
    SILVER : 'Silver',
    GOLD : 'Gold',
    PLATINUM : 'Platinum'
};

var Response = {
    
    success : function(context, user, isNewUser){
        console.log('Success: ' + JSON.stringify(user) + " " + isNewUser);
        var ending = user.name ? ", " + user.name : "";
        if (isNewUser){
            context.succeed('Thank you for joining our service' + ending + '.');  // Echo back the response
        } else {
            context.succeed('Thank you for being a '+ user.status +' member' + ending + '.');  // Echo back the response
        }
    },
    
    error : function(context, err){
        console.log('Error: ' + err);
        context.succeed('Error in request: ' + err);  // Echo back the response
    }
    
};

var Utils = {
    
    isEmpty : function(obj) {
      return Object.keys(obj).length === 0;
    },
    
    isNumber : function(n) {
        return typeof(n) === 'number';
    },
    
    dbWrap : function(o){
        var o2 = {}
        for (var key in o) {
            var val = o[key];
            if (val){
                console.log(key + ": " + val + " " + Utils.isNumber(val));
                if (Utils.isNumber(val)){
                    val = {N : val + ""};   
                } else {
                    val = {S : val};   
                }
                o2[key] = val;
            }
        }
        return o2;
    },
    
    dbUnwrap : function(o){
        var o2 = {}
        for (var key in o) {
          if (o.hasOwnProperty(key)) {
            var val = o[key]['S'] ? o[key]['S'] : o[key]['N']
            if (val){
                o2[key] = val;
            }
          }
        }
        return o2;
    },
}

exports.handler = function(event, context) {

    console.log('Received event:' + JSON.stringify(event));
    
    var phoneNumber = event.phoneNumber;
    var id = event.id;
    var accessToken = event.accessToken;
    var accessTokenSecret = event.accessTokenSecret;
    
    User.get(phoneNumber, function(err, user) {
        console.log("getUser: " + JSON.stringify(err) + " " + JSON.stringify(user));
        if (err) {
            Response.error(context, err);
        } else {
            if (Utils.isEmpty(user)){
                User.insert(phoneNumber, id, accessToken, accessTokenSecret, "", Status.SILVER, function(err, user) {
                    console.log("saveUser: " + JSON.stringify(err) + " " + JSON.stringify(user));
                    if (err) {
                        Response.error(context, err);
                    } else {
                        Response.success(context, Utils.dbUnwrap(user.Item), true);
                    }
                });
            } else {
                Response.success(context, Utils.dbUnwrap(user.Item), false);
            }
        }
    });
    
};
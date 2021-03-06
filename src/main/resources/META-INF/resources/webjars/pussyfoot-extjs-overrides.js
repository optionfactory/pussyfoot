Ext.form.Action.prototype.processResponse = function (decoded) {
    return (this.result = decoded);
};

function callbackByHttpStatus(response) {
    var decoded = response.responseText === "" ? undefined : Ext.decode(response.responseText, true);
    if (decoded === null) {
        this.onFailure(response);
        return;
    }
    if (this.isUpload || (this.form && this.form.hasUpload && this.form.hasUpload())) {
        //we get no response status from uploads so all we can do is check if it looks like an error.
        var looksLikeAnError = decoded
                && decoded.constructor === Array
                && decoded.length > 0
                && decoded[0] !== null
                && decoded[0] !== undefined
                && 'context' in decoded[0]
                && 'reason' in decoded[0]
                && 'details' in decoded[0];
        response.status = looksLikeAnError ? 400 : 200;
    }
    var transformed = Math.floor(response.status / 100) === 2
            ? {success: true, data: decoded}
    : {success: false, errors: decoded};
    this.onSuccess(transformed);
}

Ext.form.Action.prototype.createCallback = function () {
    return {
        success: callbackByHttpStatus.bind(this),
        failure: callbackByHttpStatus.bind(this),
        scope: this,
        timeout: (this.timeout * 1000) || (this.form.timeout * 1000)
    };
};

Ext.data.proxy.Server.prototype.config.pageParam = '';
Ext.data.proxy.Server.prototype.config.sortParam = 'sorters';
Ext.data.proxy.Server.prototype.config.filterParam = 'filters';

Ext.grid.filters.filter.Number.prototype.getSerializer = function () {
    return function (data) {
        data.value = JSON.stringify({value: data.value, operator: data.operator});
        delete data.operator;
    };
};
Ext.grid.filters.filter.Date.prototype.getSerializer = function () {
    return function (data) {
        data.value = JSON.stringify({value: new Date(data.value).getTime(), operator: data.operator});
        delete data.operator;
    };
};
Ext.grid.filters.filter.List.prototype.getSerializer = function (o) {
    return function (data) {
        data.value = JSON.stringify(data.value);
        delete data.operator;
    }

}
Ext.define('Ext.grid.filters.filter.UTCDate', {
    extend: 'Ext.grid.filters.filter.Date',
    alias: 'grid.filter.utcdate',
    type: 'date',
    getSerializer: function () {
        return function (data) {
            var d = new Date(data.value);
            var utcTimestamp = new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate(), 0, 0, 0)).getTime();
            data.value = JSON.stringify({value: utcTimestamp, operator: data.operator});
            delete data.operator;
        };
    }
});

/*
 * usage:
 *   filter: {
 *       type: 'utcDateWithTimeZone',
 *       timeZone: jstz.determine().name()
 *   }
 * 
 */
Ext.define('Ext.grid.filters.filter.UTCDateWithTimeZone', {
    extend: 'Ext.grid.filters.filter.Date',
    alias: 'grid.filter.utcDateWithTimeZone',
    type: 'date',
    constructor: function (conf) {
        var me = this;
        if(!conf.hasOwnProperty("timeZone")){
            throw 'Field "timeZone" is required';
        }
        var defaultConf = {
            getSerializer: function () {
                return function (data) {
                    var d = new Date(data.value);
                    var utcTimestamp = new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate(), 0, 0, 0)).getTime();
                    data.value = JSON.stringify({value: utcTimestamp, operator: data.operator, timeZone: me.timeZone});
                    delete data.operator;
                };
            }
        };
        this.callParent([Ext.merge({}, defaultConf, conf)]);
    }
});

/*
 * We always use submit as json (except for uploads)
 */
Ext.form.action.Submit.prototype.jsonSubmit = true;


/*
 * Sorters are serialized as {"name": "name", "direction": "ASC"}
 */
Ext.util.Sorter.prototype.serialize = function () {
    return {
        name: this.getProperty(), /*!*/
        direction: this.getDirection()
    };
}
/*
 * Filters are serialized as {"name": "name", "value": "something"}
 */
Ext.util.Filter.prototype.serialize = function () {
    var result = this.getState();
    var serializer = this.getSerializer();
    delete result.id;
    delete result.serializer;
    if (serializer) {
        serializer.call(this, result);
    }
    result.name = result.property; /*!*/
    delete result.property;
    return result;
};



/*
 * Errors are serialized back in this form: 
 * [
 *  {"context":"", "reason":"", "details":"" }
 * ]
 */
Ext.form.Basic.prototype.markInvalid = function (errors) {
    var me = this,
            e, eLen, error, value,
            key;

    function mark(fieldId, msg) {
        var field = me.findField(fieldId);
        if (field) {
            field.markInvalid(msg);
        }
    }

    if (Ext.isArray(errors)) {
        eLen = errors.length;

        for (e = 0; e < eLen; e++) {
            error = errors[e];
            //was: mark(error.id || error.field, error.msg || error.message);
            mark(error.context, error.reason);
        }
    } else if (errors instanceof Ext.data.Errors) {
        eLen = errors.items.length;
        for (e = 0; e < eLen; e++) {
            error = errors.items[e];

            mark(error.field, error.message);
        }
    } else {
        for (key in errors) {
            if (errors.hasOwnProperty(key)) {
                value = errors[key];
                mark(key, value, errors);
            }
        }
    }
    return this;
};

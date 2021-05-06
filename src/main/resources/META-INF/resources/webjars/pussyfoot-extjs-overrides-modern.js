Ext.data.proxy.Server.prototype.config.pageParam = '';
Ext.data.proxy.Server.prototype.config.filterParam = 'filters';
Ext.data.proxy.Server.prototype.config.sortParam = 'sorters';

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

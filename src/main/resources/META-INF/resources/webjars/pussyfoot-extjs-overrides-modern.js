/* global Ext */

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
 * QueryParam Filters for combobox are serialized as {"operator":"like","value":"something","name":"property"}
 */
serializePrimaryFilter = function (filter)
{
    var result = filter.getState();
    var serializer = filter.getSerializer();
    delete result.id;
    delete result.serializer;
    delete result.disabled;
    delete result.anyMatch;
    delete result.caseSensitive;
    if (serializer) {
        serializer.call(filter, result);
    }
    result.operator = 'like';
    result.name = Ext.Object.isEmpty(result.value) ? '' : result.property; /*!*/
    delete result.property;
    return result;
};

Ext.field.ComboBox.prototype.serializePrimaryFilter = serializePrimaryFilter;
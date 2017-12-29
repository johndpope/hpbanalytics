/**
 * Created by robertk on 4/17/2015.
 */
Ext.define('HanGui.view.iblogger.IbLoggerModel', {
    extend: 'Ext.app.ViewModel',
    requires: [
        'HanGui.model.iblogger.IbOrder',
        'HanGui.model.iblogger.Position',
        'HanGui.model.iblogger.IbAccount'
    ],

    alias: 'viewmodel.han-iblogger',

    stores: {
        ibOrders: {
            model: 'HanGui.model.iblogger.IbOrder',
            autoload: true,
            pageSize: 25,
            remoteFilter: true,
            remoteSort: false
        },
        positions: {
            model: 'HanGui.model.iblogger.Position',
            autoload: true,
            pageSize: 25
        },
        ibAccounts: {
            model: 'HanGui.model.iblogger.IbAccount',
            autoload: true,
            pageSize: 10
        }
    }
});
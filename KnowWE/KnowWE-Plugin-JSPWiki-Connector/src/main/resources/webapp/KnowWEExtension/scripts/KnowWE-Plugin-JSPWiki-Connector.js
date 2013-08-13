if (typeof KNOWWE == "undefined" || !KNOWWE) {
    var KNOWWE = {};
}

/**
 * The KNOWWE.core global namespace object. If KNOWWE.core is already defined,
 * the existing KNOWWE.core object will not be overwritten so that defined
 * namespaces are preserved.
 */
if (typeof KNOWWE.plugin == "undefined" || !KNOWWE.plugin) {
    KNOWWE.plugin = {};
}

KNOWWE.plugin.jspwikiConnector = {};

KNOWWE.plugin.jspwikiConnector.setReadOnly = function(checkbox) {

    var params = {
        action: 'ReadOnlyAction',
        readonly: jq$(checkbox).prop('checked'),
    };

    var options = {
        url: KNOWWE.core.util.getURL(params),
        response: {
            action: 'none',
            fn: function() {
            	_EC.executeIfPrivileged(KNOWWE.plugin.jspwikiConnector.enableEditButtons, KNOWWE.plugin.jspwikiConnector.disableEditButtons);
            },
            onError: _EC.onErrorBehavior,
        }
    };
    new _KA(options).send();
}

KNOWWE.plugin.jspwikiConnector.enableEditButtons = function() {
	jq$('#actionsTop').find('.edit').parent().show();
	_IE.enableDefaultEditTool();
	_EM.changeActionMenu();
}

KNOWWE.plugin.jspwikiConnector.disableEditButtons = function() {
	jq$('#actionsTop').find('.edit').parent().hide();
	_IE.disableDefaultEditTool();
}

jq$(document).ready(function() {
	_EC.executeIfPrivileged(KNOWWE.plugin.jspwikiConnector.enableEditButtons, KNOWWE.plugin.jspwikiConnector.disableEditButtons);
});
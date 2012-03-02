Guard.prototype.toXML = function(rule) {
	var condition = this.getConditionString();

	if (this.markup == 'timeDB') {
		//test for short condition (left out lhs with binary op)
		var regex =/^eval\(\s*(<|<=|>|>=|!=|=)(.*)\)/i;
		var match = regex.exec(condition);
		if (match) {
			var nodeModel = rule.getSourceNode().getNodeModel();
			
			if (!nodeModel.action) return condition;
			
			var action = new Action(nodeModel.action.markup, nodeModel.action.expression);
			
			return "eval("+action.getInfoObjectName() + " " + match[1] + match[2] + ")";
		}
		
	}
	
	return condition;

}

/**
 * GuardEditor
 * 
 * Class for editing an existing guard with a given set of 
 * possible guards. 
 */

 function GuardEditor(parent, initialGuard, possibleGuards, onChangeListener) {
 	this.parent = $(parent);
 	this.initialGuard = initialGuard;
 	this.possibleGuards = possibleGuards || [];
 	this.onChangeListener = onChangeListener;
 	this.values = [];
 
 	this.dom = null;
 	this.selectedIndex = -1;
 	
 	if (this.initialGuard) {
	 	for (var i=this.possibleGuards.length -1; i > 0; i--) {
	 		var guard = this.possibleGuards[i];
	 		if (DiaFluxUtils.isString(guard)) continue;
	 		// das erste selektieren, dass fuer den Guard Vorlage ist 
	 		// (bzw. einfach nur das erste wenn wir keinen Guard haben)
	 		if (guard.isPatternFor(this.initialGuard)) {
	 			this.selectedIndex = i;
		 		this.values = this.initialGuard.getValues(guard);
	 		}
	 	}
	 	if (this.selectedIndex == -1) {
	 		if (DiaFluxUtils.isString(this.initialGuard)) {
	 			this.initialGuard = new Guard('KnOffice', this.initialGuard, this.initialGuard);
	 		}
	 		this.possibleGuards.unshift(this.initialGuard);
	 		this.selectedIndex = 0;
	 	}
 	} else { // if no initial guard is present, choose last one. this would be TRUE in most cases.
 		if (this.possibleGuards){
 			this.selectedIndex = this.possibleGuards.length - 1;
 		}
 	}
 	 	
	this.setVisible(true);
	this.handleValueSelected();
}

GuardEditor.prototype.getDOM = function() {
	return this.dom;
}

GuardEditor.prototype.isVisible = function() {
	return (this.dom != null);
}

/**
 * Returns the selected guard. 
 * 
 * If the guard has any variables inside, the current values are injected.
 */
GuardEditor.prototype.getGuard = function() {
	var guard = this.getSelectedGuard();
	if (guard) {
		guard = new Guard(guard.getMarkup(), guard.getConditionString(), guard.getDisplayHTML(), guard.unit);
		// update input fields and inject them into the copy of selected guard
		guard.inject(this.values);
	}
	return guard;
}

/**
 * Returns the selected guard. 
 * 
 * In contrast to "getGuard()" you will get the selected guard prototype
 * as it is, instead of a guard with the edited values injected instead.
 * (e.g. for numeric comparisons)
 */
GuardEditor.prototype.getSelectedGuard = function() {
	return this.possibleGuards[this.selectedIndex];
}

/**
 * Is called after selecting a new Guard-Template. Creates new guard according to entered values 
 * and refreshes the input fields.
 */
GuardEditor.prototype.handleValueSelected = function() {
	if (!this.isVisible()) return;
	this.updateFromUI();
	this.updateInputField();
	if (this.onChangeListener) this.onChangeListener(this.getGuard());
}

/**
 * Is called after input into textfield/-area. Creates new guard according to entered values 
 */
GuardEditor.prototype.handleInput = function() {
	if (!this.isVisible()) return;
	this.updateFromUI();
	if (this.onChangeListener) this.onChangeListener(this.getGuard());
}



GuardEditor.prototype.updateFromUI = function() {
	if (!this.isVisible()) return;
	var select = this.dom.select('.value select')[0];
	this.selectedIndex = select.value;
	var inputs = this.dom.select('.inputParent .input');
	var guard = this.getSelectedGuard();
	var count = guard ? guard.countVariables() : 0;
	this.values = [];
	var allValues = [];
	for (var i=0; i<inputs.length; i++) {
		if (i<count) this.values.push(inputs[i].value);
		allValues.push(inputs[i].value);
	}
	//also update selection entries if they need an inject
	var optIndex = 0;
	for (var i=0; i<this.possibleGuards.length; i++) {
		if (DiaFluxUtils.isString(this.possibleGuards[i])) continue;
		select.options[optIndex++].innerHTML = 
			this.possibleGuards[i].getDisplayHTML(allValues);
	}
}

GuardEditor.prototype.updateInputField = function() {
	var guard = this.getSelectedGuard();
	var count = guard ? guard.countVariables() : 0;
	var inputParent = this.dom.select('.inputParent')[0];
	var inputs = this.createInputFields();
	inputParent.update(''); //remove old inputs
	
	for (var i = 0; i < inputs.length; i++){
		inputParent.insert(inputs[i]);
		if (i < count) {
			inputs[i].show();
			//focuses the first of the inputs
			if (i == 0){
				inputs[0].focus();
				inputs[0].select();
			}
		}
		else {
			inputs[i].hide();
		}
	}
}

GuardEditor.prototype.focus = function() {
	var select = this.dom.select('select')[0];
	var inputFocuses = this.dom.select('.inputFocus');
	if (select.visible()) {
		select.focus();
	}
	else if (inputFocuses.length > 0) {
		inputFocuses[0].focus();
	}
}

GuardEditor.prototype.setVisible = function(visible) {
	if (!this.isVisible() && visible) {
		// ==> show Node
		this.dom = this.render();
		this.parent.appendChild(this.dom);
		this.updateInputField();
		this.focus();
	}
	else if (this.isVisible() && !visible) {
		// ==> hide Node
		this.parent.removeChild(this.dom);
		this.dom = null;
	}
}

GuardEditor.prototype.render = function() {
	var select = 
		'<select class="guard " ' +
		(this.possibleGuards.length==0 ? 'style="display:none;" ' : '') +
		'onchange="this.parentNode.parentNode.__GuardEditor.handleValueSelected();">';
	for (var i=0; i<this.possibleGuards.length; i++) {
		var guard = this.possibleGuards[i];
 		if (DiaFluxUtils.isString(guard)) {
 			select += '<optgroup label="--- '+guard+' ---"></optgroup>';
 		}
 		else {
			select += 
				'<option value=' + i + 
				(i == this.selectedIndex ? ' selected': '') +
				'>' + guard.getDisplayHTML() + '</option>';
 		}
	}
	select += '</select>';
	
	if (this.possibleGuards.length==0) {
		select += '<a href="#" class="inputFocus"></a>';
	}
	
	var selectParent, inputParent, inputs;
	inputs = this.createInputFields(); 
	
	var dom = Builder.node('div', {className: 'GuardEditor '}, 
	[
		selectParent = Builder.node('div', {className: 'value'}),	// value dropdown parent
		inputParent = Builder.node('div', {className: 'inputParent GuardEditorEventHandler'}, inputs)
	]);
	dom.__GuardEditor = this;
	selectParent.innerHTML = select;
	return dom;
}

GuardEditor.prototype.createInputFields = function(){
	var guard = this.getGuard();
	
	if (guard && guard.isFormula()) {
		return [
		    Builder.node('textarea', {className: 'input formula', style: 'display: none;', onBlur: 'this.parentNode.parentNode.__GuardEditor.handleInput();', rows: 1},[this.values.length > 0 ? this.values[0] : '']),
		    //this one should never be visible, but avoids to see '${num} when switching from formula to interval
		    Builder.node('input', {	className: 'input', type: 'text', style: 'display: none;', onBlur: 'this.parentNode.parentNode.__GuardEditor.handleValueSelected();', value: ''})
		];
	} else {
		return [
			Builder.node('input', {	className: 'input', type: 'text', style: 'display: none;', onBlur: 'this.parentNode.parentNode.__GuardEditor.handleInput();', value: this.values.length > 0 ? this.values[0] : ''}),
			Builder.node('input', {	className: 'input', type: 'text', style: 'display: none;', onBlur: 'this.parentNode.parentNode.__GuardEditor.handleInput();', value: this.values.length > 1 ? this.values[1] : ''})
		];
	}
	
}

// avoid handling key events while editing the edit fields or the select
CCEvents.addClassListener('keydown', 'GuardEditorEventHandler', function(event) {
	
	switch(event.keyCode){
	case Event.KEY_RETURN:
		this.parentNode.__GuardEditor.handleInput();
	case Event.KEY_ESC: 
		//if the editor is not hidden before setting the selection to null, the values are set by onBlurHandler
		this.parentNode.__GuardEditor.setVisible(false); 
		theFlowchart.setSelection(null);
		return;
	}
	
	event.defaultHandler();	
});



GuardEditor.prototype.destroy = function() {
	if (this._destroyed) return;
	this._destroyed = true;
	this.setVisible(false);
}
 
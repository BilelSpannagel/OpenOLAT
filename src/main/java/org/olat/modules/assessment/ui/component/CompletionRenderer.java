/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.modules.assessment.ui.component;

import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.DefaultComponentRenderer;
import org.olat.core.gui.render.RenderResult;
import org.olat.core.gui.render.Renderer;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.render.URLBuilder;
import org.olat.core.gui.translator.Translator;

/**
 * 
 * Initial date: 22 nov. 2017<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class CompletionRenderer extends DefaultComponentRenderer {

	@Override
	public void render(Renderer renderer, StringOutput sb, Component source, URLBuilder ubu, Translator translator,
			RenderResult renderResult, String[] args) {
		
		CompletionComponent ccp = (CompletionComponent) source;

		double percent = 100.0d;
		double completion = ccp.getCompletion() == null ? 0.0d : ccp.getCompletion().doubleValue();
		double percentCompletion = percent * completion;
		if(percentCompletion < 0.0d) {
			percentCompletion = 0.0d;
		} else if(percentCompletion > percent) {
			percentCompletion = percent;
		}
		
		sb.append("<div id='").append(ccp.getFormItem().getFormDispatchId()).append("'");
		if(ccp.isEnded()) {
			sb.append(">").append(ccp.getCompletionTranslator().translate("run.ended"));
		} else {
			sb.append(" class='progress' style=\"width:100%;\"><div class='progress-bar' style=\"width:")
				.append(Math.round(percentCompletion))
				.append("%\" title=\"")
				.append(Math.round(percentCompletion))
				.append("%\">")
				.append("</div>");
		}
		sb.append("</div>");
	}
}
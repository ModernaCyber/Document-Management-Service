/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.web.forms;

import org.alfresco.service.cmr.repository.NodeRef;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.List;

/**
 * Encapsulation of a form.
 *
 * @author Ariel Backenroth
 */
public interface Form
   extends Serializable
{

   /** the name of the form, which must be unique within the FormsService */
   public String getName();

   /** the description of the form */
   public String getDescription();

   /** the root tag to use within the schema */
   public String getSchemaRootElementName();

   /** the xml schema for this template type */
   public Document getSchema()
      throws IOException, SAXException;

   /** 
    * provides the output path for the form instance data based on the 
    * configured output path pattern.
    *
    * @param parentAVMPath the parent avm path
    * @param formInstanceDataFileName the file name provided by the user.
    * @param formInstanceData the parsed xml content
    *
    * @return the path to use for writing the form instance data.
    */
   public String getOutputPathForFormInstanceData(final String parentAVMPath,
                                                  final String formInstanceDataFileName,
                                                  final Document formInstanceData);

   //XXXarielb not used currently and not sure if it's necessary...
   //    public void addInputMethod(final TemplateInputMethod in);

   /**
    * Provides a set of input methods for this template.
    */
   public List<FormProcessor> getFormProcessors();

   /**
    * adds an output method to this template type.
    */
   public void addRenderingEngineTemplate(RenderingEngineTemplate output);

   /**
    * Provides the set of output methods for this template.
    */
   public List<RenderingEngineTemplate> getRenderingEngineTemplates();


   /**
    * Associates properties with the form instance data registering it as
    * generated by this form.
    */
   public void registerFormInstanceData(final NodeRef formInstanceDataNodeRef);
}

/*
 * Copyright 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.context.config.xml;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.STSSessionCredentialsProvider;
import com.amazonaws.internal.StaticCredentialsProvider;
import org.elasticspring.context.credentials.CredentialsProviderFactoryBean;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} implementation which parses the
 * &lt;context-credentials/&gt; Element
 *
 * @author Agim Emruli
 * @since 1.0
 */
class CredentialsBeanDefinitionParser extends AbstractBeanDefinitionParser {

	private static final String CREDENTIALS_PROVIDER_BEAN_NAME = AWSCredentialsProvider.class.getName();
	private static final String ACCESS_KEY_ATTRIBUTE_NAME = "access-Key";
	private static final String SECRET_KEY_ATTRIBUTE_NAME = "secret-Key";


	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) throws BeanDefinitionStoreException {
		return CREDENTIALS_PROVIDER_BEAN_NAME;
	}

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(CredentialsProviderFactoryBean.class);
		beanDefinitionBuilder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);


		if (parserContext.getRegistry().containsBeanDefinition(CREDENTIALS_PROVIDER_BEAN_NAME)) {
			parserContext.getReaderContext().error("Multiple <context-credentials/> detected. The <context-credentials/> is only allowed once per application context", element);
		}

		List<Element> elements = DomUtils.getChildElements(element);
		List<AWSCredentialsProvider> credentialsProviders = new ArrayList<AWSCredentialsProvider>(elements.size());
		for (Element credentialsProviderElement : elements) {
			if ("simple-credentials".equals(credentialsProviderElement.getNodeName())) {
				credentialsProviders.add(new StaticCredentialsProvider(getCredentials(credentialsProviderElement, parserContext)));
			}

			if ("security-token-credentials".equals(credentialsProviderElement.getNodeName())) {
				credentialsProviders.add(new STSSessionCredentialsProvider(getCredentials(credentialsProviderElement, parserContext)));
			}

			if ("simple-credentials".equals(credentialsProviderElement.getNodeName())) {
				credentialsProviders.add(new InstanceProfileCredentialsProvider());
			}
		}

		beanDefinitionBuilder.addConstructorArgValue(credentialsProviders);

		return beanDefinitionBuilder.getBeanDefinition();
	}

	private static BasicAWSCredentials getCredentials(Element credentialsProviderElement, ParserContext parserContext) {
		String accessKey = getAttributeValue(ACCESS_KEY_ATTRIBUTE_NAME, credentialsProviderElement, parserContext);
		String secretKey = getAttributeValue(SECRET_KEY_ATTRIBUTE_NAME, credentialsProviderElement, parserContext);
		return new BasicAWSCredentials(accessKey, secretKey);
	}

	/**
	 * Returns the attribute value and reports an error if the attribute value is null or empty. Normally the reported
	 * error leads into an exception which will be thrown through the {@link org.springframework.beans.factory.parsing.ProblemReporter}
	 * implementation.
	 *
	 * @param attribute
	 * 		- The name of the attribute which will be valuated
	 * @param element
	 * 		- The element that contains the attribute
	 * @param parserContext
	 * 		- The parser context used to report errors
	 * @return - The attribute value
	 */
	private static String getAttributeValue(String attribute, Element element, ParserContext parserContext) {
		String attributeValue = element.getAttribute(attribute);
		if (StringUtils.hasText(attributeValue)) {
			parserContext.getReaderContext().error("The " + attribute + "attribute must not be empty", element);
		}
		return attributeValue;
	}

}
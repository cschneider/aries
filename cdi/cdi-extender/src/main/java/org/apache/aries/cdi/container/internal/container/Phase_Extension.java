/**
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

package org.apache.aries.cdi.container.internal.container;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.inject.spi.Extension;

import org.jboss.weld.bootstrap.spi.Metadata;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.cdi.CdiConstants;
import org.osgi.service.cdi.CdiEvent;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Phase_Extension implements Phase {

	public Phase_Extension(CdiContainerState cdiContainerState) {
		_cdiContainerState = cdiContainerState;
		_bundleContext = _cdiContainerState.getBundle().getBundleContext();
		_extensionDependencies = findExtensionDependencies(
			_cdiContainerState.getBundle().adapt(BundleWiring.class));
		_extensions = new ConcurrentSkipListMap<>(Comparator.reverseOrder());

		_cdiContainerState.setExtensionDependencies(_extensionDependencies);
	}

	@Override
	public void close() {
		_cdiContainerState.fire(CdiEvent.Type.DESTROYING);

		if (_extensionTracker != null) {
			_extensionTracker.close();

			_extensionTracker = null;
		}
		else {
			_nextPhase.close();

			_nextPhase = null;
		}

		_cdiContainerState.fire(CdiEvent.Type.DESTROYED);

		_cdiContainerState.close();
	}

	@Override
	public void open() {
		_cdiContainerState.fire(CdiEvent.Type.CREATING);

		if (!_extensionDependencies.isEmpty()) {
			Filter filter = FilterBuilder.createExtensionFilter(_extensionDependencies);

			_cdiContainerState.fire(CdiEvent.Type.WAITING_FOR_EXTENSIONS, filter.toString());

			_extensionTracker = new ServiceTracker<>(_bundleContext, filter, new ExtensionPhaseCustomizer());

			_extensionTracker.open();
		}
		else {
			_nextPhase = new Phase_Configuration(_cdiContainerState, _extensions);

			_nextPhase.open();
		}
	}

	List<ExtensionDependency> findExtensionDependencies(BundleWiring bundleWiring) {
		List<ExtensionDependency> extensionDependencies = new CopyOnWriteArrayList<>();
		List<BundleWire> requiredWires = bundleWiring.getRequiredWires(CdiConstants.CDI_EXTENSION_NAMESPACE);

		for (BundleWire wire : requiredWires) {
			Map<String, Object> attributes = wire.getCapability().getAttributes();

			String extension = (String)attributes.get(CdiConstants.CDI_EXTENSION_NAMESPACE);

			if (extension != null) {
				ExtensionDependency extensionDependency = new ExtensionDependency(
					_bundleContext, wire.getProvider().getBundle().getBundleId(), extension);

				extensionDependencies.add(extensionDependency);
			}
		}

		return extensionDependencies;
	}

	private static final Logger _log = LoggerFactory.getLogger(Phase_Extension.class);

	private final BundleContext _bundleContext;
	private final CdiContainerState _cdiContainerState;
	private final Map<ServiceReference<Extension>, Metadata<Extension>> _extensions;
	private final List<ExtensionDependency> _extensionDependencies;
	private Phase _nextPhase;

	private ServiceTracker<Extension, ExtensionDependency> _extensionTracker;

	private class ExtensionPhaseCustomizer implements ServiceTrackerCustomizer<Extension, ExtensionDependency> {

		@Override
		public ExtensionDependency addingService(ServiceReference<Extension> reference) {
			ExtensionDependency trackedDependency = null;

			for (ExtensionDependency extensionDependency : _extensionDependencies) {
				if (extensionDependency.matches(reference)) {
					_extensionDependencies.remove(extensionDependency);
					trackedDependency = extensionDependency;

					Extension extension = _bundleContext.getService(reference);

					_extensions.put(reference, new ExtensionMetadata(extension, reference.getBundle().toString()));

					break;
				}
			}

			if ((trackedDependency != null) && _extensionDependencies.isEmpty()) {
				_nextPhase = new Phase_Configuration(_cdiContainerState, _extensions);

				_nextPhase.open();
			}
			else if (_log.isDebugEnabled()) {
				_log.debug("CDIe - Still waiting for extensions {}", _extensionDependencies);
			}

			return trackedDependency;
		}

		@Override
		public void modifiedService(ServiceReference<Extension> reference, ExtensionDependency extentionDependency) {
		}

		@Override
		public void removedService(ServiceReference<Extension> reference, ExtensionDependency extentionDependency) {
			if (_extensionDependencies.isEmpty()) {
				_nextPhase.close();

				_nextPhase = null;

				_cdiContainerState.fire(CdiEvent.Type.WAITING_FOR_EXTENSIONS);
			}
			_extensions.remove(reference);

			try {
				_bundleContext.ungetService(reference);
			}
			catch (IllegalStateException ise) {
				if (_log.isWarnEnabled()) {
					_log.warn("CDIe - UngetService resulted in error", ise);
				}
			}

			_extensionDependencies.add(extentionDependency);
		}

	}

}

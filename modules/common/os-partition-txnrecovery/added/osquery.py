#!/bin/python
"""
Copyright 2018 Red Hat, Inc.

Red Hat licenses this file to you under the Apache License, version
2.0 (the "License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied.  See the License for the specific language governing
permissions and limitations under the License.
"""

import logging
import urllib.request

logger = logging.getLogger(__name__)

class OpenShiftQuery():
    """
    Utility class to help query OpenShift api. Declares constant
    to get token and uri of the query. Having methods doing the query etc.
    """

    API_URL = 'https://openshift.default.svc'
    TOKEN_FILE_PATH = '/var/run/secrets/kubernetes.io/serviceaccount/token'
    NAMESPACE_FILE_PATH = '/var/run/secrets/kubernetes.io/serviceaccount/namespace'
    CERT_FILE_PATH = '/var/run/secrets/kubernetes.io/serviceaccount/ca.crt'
    STATUS_LIVING_PODS = ['Pending', 'Running', 'Unknown']

    @staticmethod
    def __readFile(fileToRead):
        with open(fileToRead, 'r') as readingfile:
            return readingfile.read().strip()

    @staticmethod
    def getToken():
        return OpenShiftQuery.__readFile(OpenShiftQuery.TOKEN_FILE_PATH)

    @staticmethod
    def getNameSpace():
        return OpenShiftQuery.__readFile(OpenShiftQuery.NAMESPACE_FILE_PATH)

    @staticmethod
    def queryApi(urlSuffix, isPretty = False):
        prettyPrintParam = '?pretty=true' if isPretty else ''
        request = urllib.request.Request(OpenShiftQuery.API_URL + urlSuffix + prettyPrintParam,
            headers = {'Authorization': 'Bearer ' + OpenShiftQuery.getToken(), 'Accept': 'application/json'})
        logger.debug('query for: "%s"', request.get_full_url())
        try:
            return urllib.request.urlopen(request, cafile = OpenShiftQuery.CERT_FILE_PATH).read()
        except:
            logger.critical('Cannot query OpenShift API for "%s"', request.get_full_url())
            raise

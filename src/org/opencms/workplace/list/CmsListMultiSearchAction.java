/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/list/CmsListMultiSearchAction.java,v $
 * Date   : $Date: 2009/08/20 11:07:45 $
 * Version: $Revision: 1.1 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (c) 2002 - 2009 Alkacon Software GmbH (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.workplace.list;

import org.opencms.i18n.CmsEncoder;
import org.opencms.util.CmsStringUtil;
import org.opencms.workplace.CmsWorkplace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Displays an advanced search form over several columns.<p>
 * 
 * @author Michael Moossen  
 * 
 * @version $Revision: 1.1 $ 
 * 
 * @since 7.6
 */
public class CmsListMultiSearchAction extends CmsListSearchAction {

    /** the html id prefix for the input element of the search bar. */
    public static final String SEARCH_COL_INPUT_ID = "listColFilter";

    /** parsed search filter. */
    private Map<String, String> m_colVals = new HashMap<String, String>();

    /**
     * Default constructor.<p>
     * 
     * @param column the first column to search in
     * 
     * @see #addColumn(CmsListColumnDefinition)
     */
    public CmsListMultiSearchAction(CmsListColumnDefinition column) {

        super(column);
    }

    /**
     * @see org.opencms.workplace.list.CmsListSearchAction#barHtml(org.opencms.workplace.CmsWorkplace)
     */
    public String barHtml(CmsWorkplace wp) {

        if (wp == null) {
            wp = getWp();
        }
        StringBuffer html = new StringBuffer(1024);
        html.append("\t\t<input type='hidden' name='");
        html.append(SEARCH_BAR_INPUT_ID);
        html.append("' id='");
        html.append(SEARCH_BAR_INPUT_ID);
        html.append("' value='");
        // http://www.securityfocus.com/archive/1/490498: searchfilter cross site scripting vulnerability:
        html.append(CmsStringUtil.escapeJavaScript(CmsEncoder.escapeXml(getSearchFilter())));
        html.append("' >\n");
        Iterator it = getColumns().iterator();
        while (it.hasNext()) {
            CmsListColumnDefinition colDef = (CmsListColumnDefinition)it.next();
            html.append("\t\t<input type='text' name='");
            html.append(SEARCH_COL_INPUT_ID).append(colDef.getId());
            html.append("' id='");
            html.append(SEARCH_COL_INPUT_ID).append(colDef.getId());
            html.append("' value='");
            String val = m_colVals.get(colDef.getId());
            if (val == null) {
                val = "";
            }
            html.append(CmsStringUtil.escapeJavaScript(CmsEncoder.escapeXml(val)));
            html.append("' title='");
            html.append(CmsStringUtil.escapeJavaScript(colDef.getName().key(wp.getLocale())));
            html.append("' size='20' maxlength='245' style='vertical-align: bottom;' >\n");
        }
        html.append(buttonHtml(wp));
        if (getShowAllAction() != null) {
            html.append("&nbsp;&nbsp;");
            html.append(getShowAllAction().buttonHtml());
        }
        // load script
        html.append("<script type=\"text/javascript\">\n");
        html.append("var LIST_SEARCH_DATA = {\n");
        html.append("SEARCH_BAR_INPUT_ID: \"").append(SEARCH_BAR_INPUT_ID).append("\",\n");
        html.append("WORKPLACE_SKIN_URI: \"").append(CmsWorkplace.getSkinUri()).append("\",\n");
        html.append("FORM: \"").append(getListId() + "-form").append("\",\n");
        html.append("COLUMNS: [");
        it = getColumns().iterator();
        while (it.hasNext()) {
            CmsListColumnDefinition colDef = (CmsListColumnDefinition)it.next();
            html.append("\"").append(colDef.getId()).append("\"");
            if (it.hasNext()) {
                html.append(", ");
            }
        }
        html.append("]\n");
        html.append("};\n");
        html.append("</script>\n");
        html.append("<script type=\"text/javascript\" src=\"");
        html.append(CmsWorkplace.getSkinUri()).append("jquery/packed/jquery.js");
        html.append("\"></script>\n");
        html.append("<script type=\"text/javascript\" src=\"");
        html.append(CmsWorkplace.getSkinUri()).append("admin/javascript/list-multisearch.js");
        html.append("\"></script>\n");

        return html.toString();
    }

    /**
     * @see org.opencms.workplace.list.CmsListSearchAction#filter(java.util.List)
     */
    @Override
    public List filter(List items) {

        if (CmsStringUtil.isEmptyOrWhitespaceOnly(getSearchFilter())) {
            return items;
        }
        List res = new ArrayList();
        Iterator itItems = items.iterator();
        while (itItems.hasNext()) {
            CmsListItem item = (CmsListItem)itItems.next();
            if (res.contains(item)) {
                continue;
            }
            boolean matched = true;
            Iterator itCols = getColumns().iterator();
            while (matched && itCols.hasNext()) {
                CmsListColumnDefinition col = (CmsListColumnDefinition)itCols.next();
                if (item.get(col.getId()) == null) {
                    matched = false;
                }
                String colFilter = m_colVals.get(col.getId());

                if ((colFilter != null) && (item.get(col.getId()).toString().indexOf(colFilter) < 0)) {
                    matched = false;
                }
            }
            if (matched) {
                res.add(item);
            }
        }
        return res;
    }

    /**
     * @see org.opencms.workplace.list.CmsListSearchAction#setSearchFilter(java.lang.String)
     */
    @Override
    public void setSearchFilter(String filter) {

        super.setSearchFilter(filter);
        m_colVals = CmsStringUtil.splitAsMap(getSearchFilter(), "|", "#");
    }
}
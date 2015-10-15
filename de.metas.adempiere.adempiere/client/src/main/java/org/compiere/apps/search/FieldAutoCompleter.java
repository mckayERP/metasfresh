package org.compiere.apps.search;

/*
 * #%L
 * de.metas.adempiere.adempiere.client
 * %%
 * Copyright (C) 2015 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */


import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeListener;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import org.adempiere.exceptions.DBException;
import org.adempiere.util.Check;
import org.adempiere.util.StringUtils;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

/**
 * @author Santhosh Kumar T - santhosh@in.fiorano.com <li>Initial contribution - http ://www.jroller.com/santhosh/date/20050620#file_path_autocompletion
 * @author Teo Sarca <li>added timed triggering <li>refactored <li>friendly database lookup
 * @author Cristina Ghita , www.arhipac.ro <li>refactored
 */
public abstract class FieldAutoCompleter implements MouseListener
{
	private static final String AUTOCOMPLETER = FieldAutoCompleter.class.getCanonicalName() + "AUTOCOMPLETER"; // NOI18N
	private static final int PopupDelayMillis = 500;
	/** Minimum chars required to popup */
	public static final int DEFAULT_PopupMinimumChars = 3;
	private int m_popupMinimumChars = DEFAULT_PopupMinimumChars;

	public static final String ITEM_More = "...";

	public static final int DEFAULT_MaxItems = 7;
	private int m_maxItems = DEFAULT_MaxItems;

	protected final CLogger log = CLogger.getCLogger(getClass());

	@SuppressWarnings("rawtypes")
	final JList listBox = new JList();

	final protected JTextComponent textBox;
	final Color bgColorDefault;
	final Color bgColorNotMatched = new Color(230, 200, 200);
	final private JPopupMenu popup = new JPopupMenu();

	private final Timer timer = new Timer(PopupDelayMillis,
			new ActionListener()
	{
		@Override
		public void actionPerformed(final ActionEvent e)
		{
			showPopup();
		}
	});

	public FieldAutoCompleter(final JTextComponent comp)
	{
		textBox = comp;
		bgColorDefault = textBox.getBackground();
		textBox.putClientProperty(AUTOCOMPLETER, this);

		final JScrollPane scroll = new JScrollPane(listBox);
		scroll.setBorder(null);

		listBox.setFocusable(false);
		listBox.addMouseListener(this);
		setMaxItems(DEFAULT_MaxItems);
		scroll.getVerticalScrollBar().setFocusable(false);
		scroll.getHorizontalScrollBar().setFocusable(false);

		popup.setBorder(BorderFactory.createLineBorder(Color.black));
		popup.add(scroll);

		if (textBox instanceof JTextField)
		{
			textBox.registerKeyboardAction(showAction, KeyStroke.getKeyStroke(
					KeyEvent.VK_DOWN, 0), JComponent.WHEN_FOCUSED);
			textBox.getDocument().addDocumentListener(documentListener);
		}

		textBox.registerKeyboardAction(upAction, KeyStroke.getKeyStroke(
				KeyEvent.VK_UP, 0), JComponent.WHEN_FOCUSED);
		textBox.registerKeyboardAction(hidePopupAction, KeyStroke.getKeyStroke(
				KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_FOCUSED);
		popup.addPopupMenuListener(new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible(final PopupMenuEvent e)
			{
			}

			@Override
			public void popupMenuWillBecomeInvisible(final PopupMenuEvent e)
			{
				textBox.unregisterKeyboardAction(KeyStroke.getKeyStroke(
						KeyEvent.VK_ENTER, 0));
			}

			@Override
			public void popupMenuCanceled(final PopupMenuEvent e)
			{
			}
		});
		listBox.setRequestFocusEnabled(false);
	}

	private static final Action acceptAction = new AbstractAction()
	{
		private static final long serialVersionUID = -3950389799318995148L;

		@Override
		public void actionPerformed(final ActionEvent e)
		{
			final JComponent tf = (JComponent)e.getSource();
			final FieldAutoCompleter completer = (FieldAutoCompleter)tf
					.getClientProperty(AUTOCOMPLETER);
			if (!completer.isEnabled())
			{
				return;
			}
			completer.popup.setVisible(false);
			if (completer.listBox.getSelectedValue() == null)
			{
				final String txt = completer.textBox.getText();

				@SuppressWarnings("rawtypes")
				final ListModel lm = completer.listBox.getModel();

				for (int index = 0; index < lm.getSize(); index++)
				{
					if (startsWithIgnoreCase(lm.getElementAt(index).toString(), txt))
					{
						completer.acceptedListItem(lm.getElementAt(index));
						break;
					}
				}
			}
			else
			{
				completer
				.acceptedListItem(completer.listBox.getSelectedValue());
			}
		}
	};

	private final DocumentListener documentListener = new DocumentListener()
	{
		@Override
		public void insertUpdate(final DocumentEvent e)
		{
			showPopupDelayed();
		}

		@Override
		public void removeUpdate(final DocumentEvent e)
		{
			showPopupDelayed();
		}

		@Override
		public void changedUpdate(final DocumentEvent e)
		{
		}
	};

	private void showPopupDelayed()
	{
		if (!isEnabled())
		{
			return;
		}
		log.finest("showPopupDelayed..");

		// Popup only if we a minimum number of characters - 2009_0017_AP1_G42_CR048
		final String search = textBox.getText();
		if (search != null && search.trim().length() < m_popupMinimumChars)
		{
			popup.setVisible(false);
			return;
		}
		//
		timer.setRepeats(false);
		timer.start();
	}

	private void showPopup()
	{
		if (!isEnabled())
		{
			return;
		}
		log.finest("showPopup");

		// task 07068: only invoke later if we aren't in the EDT to start with
		if (SwingUtilities.isEventDispatchThread())
		{
			showPopUpInCurrentThread();
		}
		else
		{
			// metas: cg: task 05116 : start : use invokeLater because of possible problem with all the events that will make the content visible have been processed
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					showPopUpInCurrentThread();
				}
			});
			// metas: cg: task 05116 : end
		}
	}

	private void showPopUpInCurrentThread()
	{
		popup.setVisible(false);
		if (textBox.isEnabled() && updateListData()
				&& listBox.getModel().getSize() != 0)
		{
			if (!(textBox instanceof JTextField))
			{
				textBox.getDocument().addDocumentListener(documentListener);
			}
			textBox.registerKeyboardAction(acceptAction, KeyStroke
					.getKeyStroke(KeyEvent.VK_ENTER, 0),
					JComponent.WHEN_FOCUSED);
			final int size = listBox.getModel().getSize();
			listBox.setVisibleRowCount(size < 10 ? size : 10);

			int x = 0;
			try
			{
				// int pos = Math.min(textBox.getCaret().getDot(),
				// textBox.getCaret().getMark());
				x = textBox.getUI().modelToView(textBox, 0).x;
			}
			catch (final BadLocationException e)
			{
				// this should never happen!!!
				e.printStackTrace();
			}
			popup.setMinimumSize(new Dimension(textBox.getWidth(), 10));
			popup.show(textBox, x, textBox.getHeight());
		}
		else
		{
			popup.setVisible(false);
		}
		textBox.requestFocus();
	}

	protected void hidePopup()
	{
		popup.setVisible(false);
	}

	static Action showAction = new AbstractAction()
	{
		private static final long serialVersionUID = 8868536979000734628L;

		@Override
		public void actionPerformed(final ActionEvent e)
		{
			final JComponent tf = (JComponent)e.getSource();
			final FieldAutoCompleter completer = (FieldAutoCompleter)tf
					.getClientProperty(AUTOCOMPLETER);
			if (tf.isEnabled() && completer.isEnabled())
			{
				if (completer.popup.isVisible())
				{
					completer.selectNextPossibleValue();
				}
				else
				{
					completer.showPopup();
				}
			}
		}
	};

	private static final Action upAction = new AbstractAction()
	{
		private static final long serialVersionUID = 2200136359410394434L;

		@Override
		public void actionPerformed(final ActionEvent e)
		{
			final JComponent tf = (JComponent)e.getSource();
			final FieldAutoCompleter completer = (FieldAutoCompleter)tf
					.getClientProperty(AUTOCOMPLETER);
			if (tf.isEnabled() && completer.isEnabled())
			{
				if (completer.popup.isVisible())
				{
					completer.selectPreviousPossibleValue();
				}
			}
		}
	};

	private static final Action hidePopupAction = new AbstractAction()
	{
		private static final long serialVersionUID = -5683983067872135654L;

		@Override
		public void actionPerformed(final ActionEvent e)
		{
			final JComponent tf = (JComponent)e.getSource();
			final FieldAutoCompleter completer = (FieldAutoCompleter)tf
					.getClientProperty(AUTOCOMPLETER);
			if (tf.isEnabled() && completer.isEnabled())
			{
				completer.popup.setVisible(false);
			}
		}
	};

	/**
	 * Selects the next item in the list. It won't change the selection if the currently selected item is already the last item.
	 */
	protected void selectNextPossibleValue()
	{
		final int si = listBox.getSelectedIndex();

		if (si < listBox.getModel().getSize() - 1)
		{
			listBox.setSelectedIndex(si + 1);
			listBox.ensureIndexIsVisible(si + 1);
		}
	}

	/**
	 * Selects the previous item in the list. It won't change the selection if the currently selected item is already the first item.
	 */
	protected void selectPreviousPossibleValue()
	{
		final int si = listBox.getSelectedIndex();

		if (si > 0)
		{
			listBox.setSelectedIndex(si - 1);
			listBox.ensureIndexIsVisible(si - 1);
		}
	}

	/**
	 * Checks if str1 starts with str2 (ignores case, trim whitespaces, strip diacritics)
	 *
	 * @param str1
	 * @param str2
	 * @return true if str1 starts with str2
	 */
	protected static boolean startsWithIgnoreCase(final String str1, final String str2)
	{
		final String s1 = org.adempiere.util.StringUtils.stripDiacritics(str1.toUpperCase()).trim();
		final String s2 = org.adempiere.util.StringUtils.stripDiacritics(str2.toUpperCase()).trim();
		return s1.startsWith(s2);
	}

	/**
	 * User has selected some item in the list. Update textfield accordingly...
	 *
	 * @param selected
	 */
	protected void acceptedListItem(final Object selected)
	{
		if (selected == null || selected == ITEM_More)
		{
			setUserObject(null);
			return;
		}
		setUserObject(selected);
		textBox.setText(convertUserObjectForTextField(selected));
	}

	/**
	 * Update list model depending on the data in textfield
	 *
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected boolean updateListData()
	{
		final String search = textBox.getText();
		if (search != null && search.trim().length() < m_popupMinimumChars)
		{
			return false;
		}

		Object userObject = getUserOject();
		if (userObject != null && !isMatching(userObject, search))
		{
			setUserObject(null);
		}
		//
		final ArrayList<Object> list = new ArrayList<Object>();
		boolean truncated = false;

		//
		// Load list from database
		final ArrayList<Object> params = new ArrayList<Object>();
		final String sql = getSelectSQL(search, textBox.getCaretPosition(), params);
		if (!Check.isEmpty(sql, true))
		{
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try
			{
				pstmt = DB.prepareStatement(sql, null);
				DB.setParameters(pstmt, params);
				rs = pstmt.executeQuery();
				int i = 0;
				while (rs.next())
				{
					if (i > 0 && i > m_maxItems)
					{
						list.add(ITEM_More);
						truncated = true;
						break;
					}
					final Object o = fetchUserObject(rs);
					if (o == null)
					{
						continue;
					}

					list.add(o);
					i++;
				}
			}
			catch (final SQLException e)
			{
				throw new DBException(e, sql.toString());
			}
			finally
			{
				DB.close(rs, pstmt);
				rs = null;
				pstmt = null;
			}
		}
		
		listBox.setListData(list.toArray());

		// if there is no items on the list return false, to not show the pop-up
		if (list.isEmpty())
		{
			return false;
		}

		// If the list has only one item, but that item is not equals with
		// m_city
		// return false to not show any popup
		userObject = getUserOject();
		if (!truncated && list.size() == 1 && userObject != null
				&& list.get(0).equals(userObject))
		{
			log.finest("nothing to do 1");
			return false;
		}

		// If there is one result then we select it - CR088
		// metas: tsa: commented per
		// http://dewiki908/mediawiki/index.php/02027:_Autocomplete_bei_einem_Treffer_anpassen_%282011082210000075%29
		// if (!truncated && list.size() == 1)
		// {
		// setUserObject(list.get(0));
		// return false;
		// }

		// if first list item matched then select it
		if (isMatching(list.get(0), search))
		{
			setUserObject(list.get(0));
			return true;
		}

		// List updated, show we need to show the pop-up
		return true;
	}

	private Object m_userObject = null;

	public void setUserObject(final Object userObject)
	{
		m_userObject = userObject;
		if (m_userObject == null && !Check.isEmpty(getText(), true))
		{
			textBox.setBackground(bgColorNotMatched);
		}
		else
		{
			textBox.setBackground(bgColorDefault);
		}
		//
		textBox.setToolTipText(userObject == null ? "" : userObject.toString());
	}

	public Object getUserOject()
	{
		return m_userObject;
	}

	protected String convertUserObjectForTextField(final Object userObject)
	{
		return userObject == null ? "" : userObject.toString();
	}

	protected boolean isMatching(final Object userObject, final String search)
	{
		if (userObject == null)
		{
			return false;
		}

		final String s1 = StringUtils
				.stripDiacritics(convertUserObjectForTextField(userObject));
		final String s2 = StringUtils.stripDiacritics(search);

		return s1.equalsIgnoreCase(s2);
	}

	/**
	 *
	 * @param search
	 * @param caretPosition
	 * @param params empty list. Needs to be filled by the implementing method.
	 * @return full SELECT SQL or null on error
	 */
	protected abstract String getSelectSQL(String search, int caretPosition, List<Object> params);

	/**
	 * @return loaded object or null if object at current result set possition was not valid
	 */
	protected abstract Object fetchUserObject(ResultSet rs) throws SQLException;

	@Override
	public void mouseEntered(final MouseEvent e)
	{
		// nothing to do
	}

	@Override
	public void mouseExited(final MouseEvent e)
	{
		// nothing to do
	}

	@Override
	public void mousePressed(final MouseEvent e)
	{
		// nothing to do
	}

	@Override
	public void mouseReleased(final MouseEvent e)
	{
		// nothing to do
	}

	@Override
	public void mouseClicked(final MouseEvent e)
	{
		if (e == null || listBox.getSelectedValue().equals(ITEM_More))
		{
			setUserObject(null);
			return;
		}

		popup.setVisible(false); // 02027: tsa: hide popup when an item is selected

		final Object selected = listBox.getSelectedValue();
		setUserObject(selected);
		textBox.setText(convertUserObjectForTextField(selected));
	}

	public void addPropertyChangeListener(final PropertyChangeListener listener)
	{
		listBox.addPropertyChangeListener(listener);
	}

	public void setMaxItems(final int maxItems)
	{
		m_maxItems = maxItems;
		listBox.setVisibleRowCount(m_maxItems + 1);
	}

	public int getMaxItems()
	{
		return m_maxItems;
	}

	public String getText()
	{
		return textBox.getText();
	}

	public boolean isEnabled()
	{
		return true;
	}

	public void setPopupMinimumChars(final int popupMinimumChars)
	{
		m_popupMinimumChars = popupMinimumChars;
	}

	public int getPopupMinimumChars()
	{
		return m_popupMinimumChars;
	}
}
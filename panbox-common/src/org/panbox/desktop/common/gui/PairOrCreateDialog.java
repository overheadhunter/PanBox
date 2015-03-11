/*
 * 
 *               Panbox - encryption for cloud storage 
 *      Copyright (C) 2014-2015 by Fraunhofer SIT and Sirrix AG 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additonally, third party code may be provided with notices and open source
 * licenses from communities and third parties that govern the use of those
 * portions, and any licenses granted hereunder do not alter any rights and
 * obligations you may have under such open source licenses, however, the
 * disclaimer of warranty and limitation of liability provisions of the GPLv3 
 * will apply to all the product.
 * 
 */
package org.panbox.desktop.common.gui;

import java.util.ResourceBundle;

import javax.swing.JLabel;

import org.panbox.Settings;

/**
 *
 * @author Clemens A. Schulz <c.schulz@sirrix.com>
 */
public class PairOrCreateDialog extends javax.swing.JDialog {

	private static final long serialVersionUID = -858037589410476122L;
	private static final ResourceBundle bundle = ResourceBundle.getBundle(
			"org.panbox.desktop.common.gui.Messages", Settings.getInstance()
					.getLocale());
	
	private Boolean pairOrDevice = null; //Tri-Bool: null = aborted, true = pair, false = create

    public PairOrCreateDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        message.setVerticalAlignment(JLabel.TOP);
        setLocationRelativeTo(null);
        createButton.requestFocus();
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        message = new javax.swing.JLabel();
        createButton = new javax.swing.JButton();
        pairingButton = new javax.swing.JButton();
        abortButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        setTitle(bundle.getString("client.pairorcreate.title")); // NOI18N
        setResizable(false);

        message.setText(bundle.getString("client.pairorcreate.message")); // NOI18N
        message.setAlignmentY(TOP_ALIGNMENT);
        message.setVerticalTextPosition(javax.swing.SwingConstants.TOP);

        createButton.setText(bundle.getString("client.pairorcreate.createButton")); // NOI18N
        createButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createButtonActionPerformed(evt);
            }
        });

        pairingButton.setText(bundle.getString("client.pairorcreate.pairingButton")); // NOI18N
        pairingButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pairingButtonActionPerformed(evt);
            }
        });

        abortButton.setText(bundle.getString("client.abort")); // NOI18N
        abortButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                abortButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(message, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 92, Short.MAX_VALUE)
                        .addComponent(abortButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pairingButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(createButton)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(message, javax.swing.GroupLayout.DEFAULT_SIZE, 114, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pairingButton)
                    .addComponent(createButton)
                    .addComponent(abortButton))
                .addContainerGap())
        );

        message.getAccessibleContext().setAccessibleName(bundle.getString("client.pairorcreate.message")); // NOI18N
        createButton.getAccessibleContext().setAccessibleName(bundle.getString("client.pairorcreate.createButton")); // NOI18N
        pairingButton.getAccessibleContext().setAccessibleName(bundle.getString("client.pairorcreate.pairingButton")); // NOI18N
        abortButton.getAccessibleContext().setAccessibleName(bundle.getString("client.abort")); // NOI18N

        getAccessibleContext().setAccessibleName(bundle.getString("client.pairorcreate.title")); // NOI18N

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void pairingButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pairingButtonActionPerformed
        pairOrDevice = true;
        this.dispose();
    }//GEN-LAST:event_pairingButtonActionPerformed

    private void createButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createButtonActionPerformed
        pairOrDevice = false;
        this.dispose();
    }//GEN-LAST:event_createButtonActionPerformed

    private void abortButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_abortButtonActionPerformed
        this.dispose();
    }//GEN-LAST:event_abortButtonActionPerformed

    public Boolean isPairing() {
        return pairOrDevice;
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton abortButton;
    private javax.swing.JButton createButton;
    private javax.swing.JLabel message;
    private javax.swing.JButton pairingButton;
    // End of variables declaration//GEN-END:variables
}

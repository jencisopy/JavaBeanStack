/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
*        jenciso@javabeanstack.org
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 3 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
* MA 02110-1301  USA
 */
package org.javabeanstack.sifen.core.validation.rules.de;

import java.util.List;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.validation.RuleSet;

/**
 * Conjuntos de reglas del Documento Electrónico, en el orden del MT (grupos A a H y las reglas del validador sobre la configuración).
 *
 * <p>Es la <b>única lista a mano</b> del catálogo (D33): un conjunto nuevo se agrega acá y
 * {@code CatalogoReglasTest} falla si alguna clase del paquete que extiende {@link RuleSet} no
 * figura. El orden es el de ejecución y el de los errores.</p>
 */
public final class DERuleSets {

    /** Conjuntos estándar, en orden. */
    public static final List<RuleSet<DocumentoElectronico, ?>> STANDARD = List.of(
            new SignedFieldsRules(),
            new OperationRules(),
            new TimbradoRules(),
            new GeneralDataRules(),
            new CommercialOperationRules(),
            new AffectedObligationRules(),
            new IssuerRules(),
            new EconomicActivityRules(),
            new DEResponsibleRules(),
            new ReceiverRules(),
            new DETypeRules(),
            new InvoiceRules(),
            new PublicProcurementRules(),
            new SelfInvoiceRules(),
            new CreditDebitNoteRules(),
            new DispatchNoteRules(),
            new OperationConditionRules(),
            new CashPaymentRules(),
            new CreditPaymentRules(),
            new InstallmentRules(),
            new ItemRules(),
            new ItemValueRules(),
            new ItemVatRules(),
            new GoodsTrackingRules(),
            new NewVehicleRules(),
            new EnergyRules(),
            new InsuranceRules(),
            new InsurancePolicyRules(),
            new SupermarketRules(),
            new AdditionalDataRules(),
            new PaymentDueDateRules(),
            new TransportRules(),
            new DeparturePlaceRules(),
            new DeliveryPlaceRules(),
            new TransportVehicleRules(),
            new CarrierRules(),
            new TotalsRules(),
            new ComplementaryDataRules(),
            new CargoRules(),
            new AssociatedDocumentsRules(),
            new AssociatedDocumentRules(),
            new DEConfigRules());

    private DERuleSets() {
    }
}

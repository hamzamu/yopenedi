package com.ywesee.java.yopenedi.converter;

import com.ywesee.java.yopenedi.OpenTrans.*;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;

import static com.ywesee.java.yopenedi.converter.Utility.formatDateISO;

public class Converter {
    public boolean shouldMergeContactDetails;

    public Order orderToOpenTrans(com.ywesee.java.yopenedi.Edifact.Order order) {
        Order o = new Order();
        o.id = order.id;

        o.deliveryStartDate = dateStringToISOString(order.deliveryStartDate);
        o.deliveryEndDate = dateStringToISOString(order.deliveryEndDate);
        o.deliveryConditionCode = order.deliveryConditionCode;
        o.deliveryConditionDetails = order.deliveryConditionDetails;
        o.currencyCoded = order.currencyCoded;

        o.parties = order.parties.stream()
                .map(this::partyToOpenTrans).collect(Collectors.toCollection(ArrayList::new));
        o.orderItems = order.orderItems.stream()
                .map(this::orderItemToOpenTrans).collect(Collectors.toCollection(ArrayList::new));

        for (com.ywesee.java.yopenedi.Edifact.Party p : order.parties) {
            if (p.role != null) {
                switch (p.role) {
                    case Supplier:
                        o.supplierIdRef = p.id;
                        break;
                    case Buyer:
                        o.buyerIdRef = p.id;
                        break;
                }
            }
        }

        return o;
    }

    public Party partyToOpenTrans(com.ywesee.java.yopenedi.Edifact.Party party) {
        Party p = new Party();
        p.id = party.id;
        if (party.role != null) {
            switch (party.role) {
                case Buyer:
                    p.role = Party.Role.Buyer;
                    break;
                case Delivery:
                    p.role = Party.Role.Delivery;
                    break;
                case Supplier:
                    p.role = Party.Role.Supplier;
                    break;
                case Invoicee:
                    p.role = Party.Role.InvoiceRecipient;
                    break;
            }
        }
        p.supplierSpecificPartyId = party.supplierSpecificPartyId;
        p.name = party.name;
        p.street = party.street;
        p.city = party.city;
        p.zip = party.zip;
        p.countryCoded = party.countryCoded;
        if (shouldMergeContactDetails) {
            ContactDetail cd = new ContactDetail();
            p.contactDetails = new ArrayList<>(Collections.singletonList(cd));

            for (com.ywesee.java.yopenedi.Edifact.ContactDetail c : party.contactDetails) {
                cd.name = Converter.mergeStringForContactDetail(c.name, cd.name);
                cd.phone = Converter.mergeStringForContactDetail(c.phone, cd.phone);
                cd.email = Converter.mergeStringForContactDetail(c.email, cd.email);
                cd.fax = Converter.mergeStringForContactDetail(c.fax, cd.fax);
            }
        } else {
            p.contactDetails = party.contactDetails.stream()
                    .map(this::contactDetailToOpenTrans)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        return p;
    }

    public OrderItem orderItemToOpenTrans(com.ywesee.java.yopenedi.Edifact.OrderItem orderItem) {
        OrderItem oi = new OrderItem();
        oi.ean = orderItem.ean;
        oi.descriptionShort = orderItem.descriptionShort;
        oi.descriptionLong = orderItem.descriptionLong;
        oi.quantity = orderItem.quantity;
        oi.quantityUnit = orderItem.quantityUnit;
        oi.price = orderItem.price;
        oi.priceQuantity = orderItem.priceQuantity;
        return oi;
    }

    public ContactDetail contactDetailToOpenTrans(com.ywesee.java.yopenedi.Edifact.ContactDetail contactDetail) {
        ContactDetail cd = new ContactDetail();
        cd.name = contactDetail.name;
        cd.phone = contactDetail.phone;
        cd.email = contactDetail.email;
        cd.fax = contactDetail.fax;
        return cd;
    }

    public com.ywesee.java.yopenedi.Edifact.Invoice invoiceToEdifact(Invoice invoice) {
        com.ywesee.java.yopenedi.Edifact.Invoice i = new com.ywesee.java.yopenedi.Edifact.Invoice();
        i.referenceNumber = invoice.documentNumber;
        i.documentNumber = invoice.documentNumber;
        i.orderDate = invoice.invoiceDate;
        i.deliveryDate = invoice.deliveryEndDate;
        i.deliveryNoteNumber = invoice.deliveryNoteNumber;
        i.orderNumberForCustomer = invoice.buyerIdRef;
        i.orderNumberForSupplier = invoice.supplierIdRef;
        i.taxType = invoice.taxType;
        i.taxRate = String.valueOf(Float.parseFloat(invoice.taxRate) * 100);
        i.currencyCode = invoice.currencyCode;

        for (PaymentTerm pt : invoice.paymentTerms) {
            if (pt.discountFactor == null || pt.discountFactor == 0.0f) {
                i.dateWithoutDiscount = invoice.dateForPaymentTerm(pt);
            } else if (i.dateWithDiscount == null) {
                if (pt.discountFactor != 0.0f) {
                    i.dateWithoutDiscount = invoice.dateForPaymentTerm(pt);
                    if (pt.discountFactor != null) {
                        i.discountPercentage = BigDecimal.valueOf(pt.discountFactor * 10.0f);
                    }
                }
            }
        }

        if (invoice.totalAmount != null) {
            i.totalAmount = new BigDecimal(invoice.totalAmount);
        }
        if (invoice.netAmountOfItems != null) {
            i.netAmountOfItems = new BigDecimal(invoice.netAmountOfItems);
        }
        if (invoice.taxAmount != null) {
            i.taxAmount = new BigDecimal(invoice.taxAmount);
        }

        i.parties = invoice.parties.stream()
                .map(this::partyToEdifact)
                .collect(Collectors.toCollection(ArrayList::new));
        i.invoiceItems = invoice.invoiceItems.stream()
                .map(this::invoiceItemToEdifact)
                .collect(Collectors.toCollection(ArrayList::new));
        return i;
    }

    public com.ywesee.java.yopenedi.Edifact.InvoiceItem invoiceItemToEdifact(InvoiceItem invoiceItem) {
        com.ywesee.java.yopenedi.Edifact.InvoiceItem ii = new com.ywesee.java.yopenedi.Edifact.InvoiceItem();
        ii.lineItemId = new BigDecimal(invoiceItem.lineItemId);
        ii.ean = invoiceItem.ean;
        ii.supplierSpecificProductId = invoiceItem.supplierSpecificProductId;
        ii.buyerSpecificProductId = invoiceItem.buyerSpecificProductId;
        ii.shortDescription = invoiceItem.shortDescription;
        ii.longDescription = invoiceItem.longDescription;

        if (invoiceItem.volume != null) {
            ii.volume = BigDecimal.valueOf(invoiceItem.volume);
        }
        if (invoiceItem.weight != null) {
            ii.weight = BigDecimal.valueOf(invoiceItem.weight);
        }
        if (invoiceItem.length != null) {
            ii.length = BigDecimal.valueOf(invoiceItem.length);
        }
        if (invoiceItem.width != null) {
            ii.width = BigDecimal.valueOf(invoiceItem.width);
        }
        if (invoiceItem.depth != null) {
            ii.depth = BigDecimal.valueOf(invoiceItem.depth);
        }

        if (invoiceItem.quantity != null) {
            ii.quantity = BigDecimal.valueOf(invoiceItem.quantity);
        }
        ii.countryOfOriginCoded = invoiceItem.countryOfOriginCoded;

        ii.deliveryDate = invoiceItem.deliveryEndDate;
        if (invoiceItem.price != null) {
            ii.price = BigDecimal.valueOf(invoiceItem.price);
        }
        if (invoiceItem.priceQuantity != null) {
            ii.priceQuantity = BigDecimal.valueOf(invoiceItem.priceQuantity);
        }
        if (invoiceItem.priceLineAmount != null) {
            ii.priceLineAmount = BigDecimal.valueOf(invoiceItem.priceLineAmount);
        }

        ii.supplierOrderId = invoiceItem.supplierOrderId;
        ii.supplierOrderItemId = invoiceItem.supplierOrderItemId;
        ii.buyerOrderId = invoiceItem.buyerOrderId;
        ii.buyerOrderItemId = invoiceItem.buyerOrderItemId;
        ii.deliveryOrderId = invoiceItem.deliveryOrderId;
        ii.deliveryOrderItemId = invoiceItem.deliveryOrderItemId;

        ii.taxType = invoiceItem.taxType;
        ii.taxRate = Float.toString(invoiceItem.taxRate * 100);
        if (invoiceItem.taxAmount != null) {
            ii.taxAmount = BigDecimal.valueOf(invoiceItem.taxAmount);
        }

        ii.allowanceOrCharges = invoiceItem.allowanceOrCharges.stream()
                .map(this::allowanceOrChargesToEdifact)
                .collect(Collectors.toCollection(ArrayList::new));
        return ii;
    }

    public com.ywesee.java.yopenedi.Edifact.Party partyToEdifact(Party party) {
        com.ywesee.java.yopenedi.Edifact.Party p = new com.ywesee.java.yopenedi.Edifact.Party();
        p.id = party.id;
        if (party.role != null) {
            switch (party.role) {
                case Buyer:
                    p.role = com.ywesee.java.yopenedi.Edifact.Party.Role.Buyer;
                    break;
                case Supplier:
                    p.role = com.ywesee.java.yopenedi.Edifact.Party.Role.Supplier;
                    break;
                case Delivery:
                    p.role = com.ywesee.java.yopenedi.Edifact.Party.Role.Delivery;
                    break;
                case InvoiceRecipient:
                    p.role = com.ywesee.java.yopenedi.Edifact.Party.Role.Invoicee;
                    break;
            }
        }
        p.supplierSpecificPartyId = party.supplierSpecificPartyId;
        p.name = party.name;
        p.street = party.street;
        p.city = party.city;
        p.zip = party.zip;
        p.countryCoded = party.countryCoded;
        p.vatId = party.vatId;
        p.fiscalNumber = party.taxNumber;
        p.contactDetails = party.contactDetails.stream()
                .map(this::contactDetailToEdifact)
                .collect(Collectors.toCollection(ArrayList::new));
        return p;
    }

    public com.ywesee.java.yopenedi.Edifact.ContactDetail contactDetailToEdifact(ContactDetail contactDetail) {
        com.ywesee.java.yopenedi.Edifact.ContactDetail cd = new com.ywesee.java.yopenedi.Edifact.ContactDetail();
        cd.phone = contactDetail.phone;
        cd.name = contactDetail.name;
        cd.fax = contactDetail.fax;
        cd.email = contactDetail.email;
        return cd;
    }

    public com.ywesee.java.yopenedi.Edifact.AllowanceOrCharge allowanceOrChargesToEdifact(AllowanceOrCharge allowanceOrCharge) {
        com.ywesee.java.yopenedi.Edifact.AllowanceOrCharge aoc = new com.ywesee.java.yopenedi.Edifact.AllowanceOrCharge();
        if (allowanceOrCharge.type != null) {
            switch (allowanceOrCharge.type) {
                case Charge:
                    aoc.type = com.ywesee.java.yopenedi.Edifact.AllowanceOrCharge.Type.Charge;
                case Allowance:
                    aoc.type = com.ywesee.java.yopenedi.Edifact.AllowanceOrCharge.Type.Allowance;
            }
        }
        aoc.name = allowanceOrCharge.name;
        aoc.sequence = allowanceOrCharge.sequence;
        if (allowanceOrCharge.percentage != null) {
            aoc.percentage = BigDecimal.valueOf(allowanceOrCharge.percentage);
        }
        if (allowanceOrCharge.amount != null) {
            aoc.amount = BigDecimal.valueOf(allowanceOrCharge.amount);
        }
        return aoc;
    }

    static String dateStringToISOString(String dateString) {
        if (dateString == null) {
            return null;
        }
        DateFormat df = new SimpleDateFormat("yyyyMMdd");
        try {
            Date date = df.parse(dateString);
            return formatDateISO(date);
        } catch (ParseException e) {
            return "";
        }
    }

    static String mergeStringForContactDetail(String a, String b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        if (a.length() > b.length()) {
            return a;
        }
        return b;
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.id1212.currencyconverter.controller;

/**
 *
 * @author HP
 */
import java.io.Serializable;
import java.time.Clock;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import se.kth.id1212.currencyconverter.currency.Currency;


@Stateless
public class ConverterFacade implements Serializable {

     @PersistenceContext(unitName = "bankPU")
    private EntityManager em;

    public ConverterFacade() {
    }

    public List<Currency> listCurrencies() {
        Query query = em.createQuery("SELECT c FROM Currency c", Currency.class);
        List<Currency> currenciesFromDB = query.getResultList();
        //if(currenciesFromDB.isEmpty())
        //{
           // em.persist(new Currency("SEK",1));
            //em.persist(new Currency("INR",0.5));
            //em.persist(new Currency("GBP",7));
            //em.persist(new Currency("YEN",11));
        //}
        return currenciesFromDB;
    }

    public double convert(double fromAmount, String fromCurrency, String toCurrency) {
        double fromRate = 0;
        double toRate = 0;
        double amt=0;
        for (Currency currency : listCurrencies()) {
            if (currency.getName().equals(fromCurrency)) {
                fromRate = currency.getRate();
            }
            if (currency.getName().equals(toCurrency)) {
                toRate = currency.getRate();
            }
        }
        amt= fromAmount * (toRate / fromRate);
        System.out.println("amt"+ amt);
        return amt;
    }

}
